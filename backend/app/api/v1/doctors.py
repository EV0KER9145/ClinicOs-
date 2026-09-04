import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.doctor import Doctor
from app.models.user import User, UserRole
from app.schemas.doctor import DoctorCreate, DoctorResponse, DoctorStatusUpdate, DoctorUpdate

router = APIRouter()


@router.get("", response_model=List[DoctorResponse])
def list_doctors(
    active_only: bool = Query(False, description="Filter active doctors only"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[DoctorResponse]:
    """List all doctors belonging to the authenticated user's clinic."""
    query = db.query(Doctor).filter(Doctor.clinic_id == current_user.clinic_id)
    if active_only:
        query = query.filter(Doctor.is_active.is_(True))
    return query.order_by(Doctor.full_name.asc()).all()


@router.post("", response_model=DoctorResponse, status_code=status.HTTP_201_CREATED)
def create_doctor(
    doctor_in: DoctorCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> DoctorResponse:
    """Create a new doctor profile in the clinic (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to create doctor profiles."
        )

    doctor = Doctor(
        clinic_id=current_user.clinic_id,
        full_name=doctor_in.full_name.strip(),
        specialty=doctor_in.specialty.strip() if doctor_in.specialty else None,
        is_active=True
    )
    db.add(doctor)
    db.commit()
    db.refresh(doctor)
    return doctor


@router.get("/{doctor_id}", response_model=DoctorResponse)
def get_doctor(
    doctor_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> DoctorResponse:
    """Get a specific doctor by ID within the authenticated user's clinic."""
    doctor = db.query(Doctor).filter(
        Doctor.id == doctor_id,
        Doctor.clinic_id == current_user.clinic_id
    ).first()

    if not doctor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Doctor profile not found."
        )
    return doctor


@router.patch("/{doctor_id}", response_model=DoctorResponse)
def update_doctor(
    doctor_id: uuid.UUID,
    doctor_in: DoctorUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> DoctorResponse:
    """Update a doctor's details (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to update doctor profiles."
        )

    doctor = db.query(Doctor).filter(
        Doctor.id == doctor_id,
        Doctor.clinic_id == current_user.clinic_id
    ).first()

    if not doctor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Doctor profile not found."
        )

    update_dict = doctor_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        setattr(doctor, field, value)

    db.add(doctor)
    db.commit()
    db.refresh(doctor)
    return doctor


@router.patch("/{doctor_id}/status", response_model=DoctorResponse)
def update_doctor_status(
    doctor_id: uuid.UUID,
    status_in: DoctorStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> DoctorResponse:
    """Soft activate or deactivate a doctor (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to change doctor status."
        )

    doctor = db.query(Doctor).filter(
        Doctor.id == doctor_id,
        Doctor.clinic_id == current_user.clinic_id
    ).first()

    if not doctor:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Doctor profile not found."
        )

    doctor.is_active = status_in.is_active
    db.add(doctor)
    db.commit()
    db.refresh(doctor)
    return doctor
