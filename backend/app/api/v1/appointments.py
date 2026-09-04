import uuid
from datetime import date, datetime, timedelta, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc, select
from sqlalchemy.orm import Session, joinedload
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.appointment import Appointment, AppointmentStatus
from app.models.doctor import Doctor
from app.models.patient import Patient
from app.models.user import User
from app.schemas.appointment import (
    AppointmentCreate,
    AppointmentListResponse,
    AppointmentResponse,
    AppointmentStatusUpdate,
    AppointmentUpdate,
)

router = APIRouter()


def ensure_utc(dt: datetime) -> datetime:
    """Ensure datetime is UTC-aware for safe comparisons across DB drivers."""
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def check_doctor_conflict(
    db: Session,
    clinic_id: uuid.UUID,
    doctor_id: uuid.UUID,
    requested_start: datetime,
    duration_minutes: int,
    exclude_appointment_id: Optional[uuid.UUID] = None
):
    """Check whether doctor already has an active overlapping appointment."""
    req_start = ensure_utc(requested_start)
    req_end = req_start + timedelta(minutes=duration_minutes)

    # Query active appointments for doctor where status is SCHEDULED or CONFIRMED
    query = db.query(Appointment).filter(
        Appointment.clinic_id == clinic_id,
        Appointment.doctor_id == doctor_id,
        Appointment.status.in_([AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED])
    )

    if exclude_appointment_id:
        query = query.filter(Appointment.id != exclude_appointment_id)

    active_appts = query.all()

    for appt in active_appts:
        existing_start = ensure_utc(appt.scheduled_at)
        existing_end = existing_start + timedelta(minutes=appt.duration_minutes)

        # Overlap condition: req_start < existing_end AND req_end > existing_start
        if req_start < existing_end and req_end > existing_start:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Doctor already has an active appointment during this time window."
            )


@router.get("", response_model=AppointmentListResponse)
def list_appointments(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    start_date: Optional[date] = Query(None, description="Start date filter (YYYY-MM-DD)"),
    end_date: Optional[date] = Query(None, description="End date filter (YYYY-MM-DD)"),
    doctor_id: Optional[uuid.UUID] = Query(None, description="Filter by doctor ID"),
    patient_id: Optional[uuid.UUID] = Query(None, description="Filter by patient ID"),
    appt_status: Optional[AppointmentStatus] = Query(None, alias="status", description="Filter by status"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AppointmentListResponse:
    """List appointments for the authenticated user's clinic with date, doctor, and status filters."""
    query = db.query(Appointment).options(
        joinedload(Appointment.patient),
        joinedload(Appointment.doctor)
    ).filter(Appointment.clinic_id == current_user.clinic_id)

    if start_date:
        start_dt = datetime.combine(start_date, datetime.min.time()).replace(tzinfo=timezone.utc)
        query = query.filter(Appointment.scheduled_at >= start_dt)

    if end_date:
        end_dt = datetime.combine(end_date, datetime.max.time()).replace(tzinfo=timezone.utc)
        query = query.filter(Appointment.scheduled_at <= end_dt)

    if doctor_id:
        query = query.filter(Appointment.doctor_id == doctor_id)

    if patient_id:
        query = query.filter(Appointment.patient_id == patient_id)

    if appt_status:
        query = query.filter(Appointment.status == appt_status)

    total = query.count()
    appointments = (
        query.order_by(Appointment.scheduled_at.asc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return AppointmentListResponse(
        items=appointments,
        total=total,
        page=page,
        page_size=page_size
    )


@router.post("", response_model=AppointmentResponse, status_code=status.HTTP_201_CREATED)
def create_appointment(
    appt_in: AppointmentCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AppointmentResponse:
    """Book a new appointment."""
    # Validate Patient
    patient = db.query(Patient).filter(
        Patient.id == appt_in.patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()
    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found in your clinic."
        )
    if not patient.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot book an appointment for an archived/inactive patient."
        )

    # Validate Doctor
    doctor = db.query(Doctor).filter(
        Doctor.id == appt_in.doctor_id,
        Doctor.clinic_id == current_user.clinic_id
    ).first()
    if not doctor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Doctor not found in your clinic."
        )
    if not doctor.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot book an appointment with an inactive doctor."
        )

    # Conflict Detection
    check_doctor_conflict(
        db=db,
        clinic_id=current_user.clinic_id,
        doctor_id=appt_in.doctor_id,
        requested_start=appt_in.scheduled_at,
        duration_minutes=appt_in.duration_minutes
    )

    appointment = Appointment(
        clinic_id=current_user.clinic_id,
        patient_id=appt_in.patient_id,
        doctor_id=appt_in.doctor_id,
        scheduled_at=ensure_utc(appt_in.scheduled_at),
        duration_minutes=appt_in.duration_minutes,
        status=AppointmentStatus.SCHEDULED,
        notes=appt_in.notes.strip() if appt_in.notes else None
    )
    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment


@router.get("/{appointment_id}", response_model=AppointmentResponse)
def get_appointment(
    appointment_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AppointmentResponse:
    """Get details for a specific appointment."""
    appointment = db.query(Appointment).options(
        joinedload(Appointment.patient),
        joinedload(Appointment.doctor)
    ).filter(
        Appointment.id == appointment_id,
        Appointment.clinic_id == current_user.clinic_id
    ).first()

    if not appointment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Appointment not found."
        )
    return appointment


@router.patch("/{appointment_id}", response_model=AppointmentResponse)
def update_appointment(
    appointment_id: uuid.UUID,
    appt_in: AppointmentUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AppointmentResponse:
    """Update appointment schedule, doctor, duration, or notes."""
    appointment = db.query(Appointment).filter(
        Appointment.id == appointment_id,
        Appointment.clinic_id == current_user.clinic_id
    ).first()

    if not appointment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Appointment not found."
        )

    target_doctor_id = appt_in.doctor_id or appointment.doctor_id
    target_start = appt_in.scheduled_at or appointment.scheduled_at
    target_duration = appt_in.duration_minutes or appointment.duration_minutes

    if appt_in.patient_id:
        patient = db.query(Patient).filter(
            Patient.id == appt_in.patient_id,
            Patient.clinic_id == current_user.clinic_id
        ).first()
        if not patient or not patient.is_active:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or inactive patient."
            )

    if appt_in.doctor_id:
        doctor = db.query(Doctor).filter(
            Doctor.id == appt_in.doctor_id,
            Doctor.clinic_id == current_user.clinic_id
        ).first()
        if not doctor or not doctor.is_active:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or inactive doctor."
            )

    # Conflict re-check if schedule or doctor changed
    if appt_in.scheduled_at or appt_in.duration_minutes or appt_in.doctor_id:
        check_doctor_conflict(
            db=db,
            clinic_id=current_user.clinic_id,
            doctor_id=target_doctor_id,
            requested_start=target_start,
            duration_minutes=target_duration,
            exclude_appointment_id=appointment_id
        )

    update_dict = appt_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        if field == "scheduled_at" and value is not None:
            value = ensure_utc(value)
        setattr(appointment, field, value)

    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment


@router.patch("/{appointment_id}/status", response_model=AppointmentResponse)
def update_appointment_status(
    appointment_id: uuid.UUID,
    status_in: AppointmentStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> AppointmentResponse:
    """Update appointment status (SCHEDULED -> CONFIRMED -> COMPLETED, CANCELLED, NO_SHOW)."""
    appointment = db.query(Appointment).filter(
        Appointment.id == appointment_id,
        Appointment.clinic_id == current_user.clinic_id
    ).first()

    if not appointment:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Appointment not found."
        )

    appointment.status = status_in.status
    db.add(appointment)
    db.commit()
    db.refresh(appointment)
    return appointment
