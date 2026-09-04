import uuid
from datetime import date, datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc, asc, or_, case
from sqlalchemy.orm import Session, joinedload
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead
from app.models.patient import Patient
from app.models.user import User
from app.schemas.follow_up import (
    FollowUpCreate,
    FollowUpListResponse,
    FollowUpResponse,
    FollowUpUpdate,
)

router = APIRouter()


def ensure_utc(dt: datetime) -> datetime:
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


@router.get("", response_model=FollowUpListResponse)
def list_follow_ups(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    fu_status: Optional[FollowUpStatus] = Query(None, alias="status", description="Filter by status"),
    assigned_user_id: Optional[uuid.UUID] = Query(None, description="Filter by assigned staff member"),
    patient_id: Optional[uuid.UUID] = Query(None, description="Filter by patient"),
    lead_id: Optional[uuid.UUID] = Query(None, description="Filter by lead"),
    overdue_only: bool = Query(False, description="Filter overdue pending follow-ups only"),
    due_from: Optional[date] = Query(None, description="Due date start range"),
    due_to: Optional[date] = Query(None, description="Due date end range"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpListResponse:
    """List follow-ups for the authenticated user's clinic with priority ordering."""
    query = db.query(FollowUp).options(
        joinedload(FollowUp.assigned_user),
        joinedload(FollowUp.patient),
        joinedload(FollowUp.lead)
    ).filter(FollowUp.clinic_id == current_user.clinic_id, FollowUp.is_active.is_(True))

    if fu_status:
        query = query.filter(FollowUp.status == fu_status)

    if assigned_user_id:
        query = query.filter(FollowUp.assigned_user_id == assigned_user_id)

    if patient_id:
        query = query.filter(FollowUp.patient_id == patient_id)

    if lead_id:
        query = query.filter(FollowUp.lead_id == lead_id)

    now_utc = datetime.now(timezone.utc)

    if overdue_only:
        query = query.filter(
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at < now_utc
        )

    if due_from:
        start_dt = datetime.combine(due_from, datetime.min.time()).replace(tzinfo=timezone.utc)
        query = query.filter(FollowUp.due_at >= start_dt)

    if due_to:
        end_dt = datetime.combine(due_to, datetime.max.time()).replace(tzinfo=timezone.utc)
        query = query.filter(FollowUp.due_at <= end_dt)

    total = query.count()

    # Priority ordering: PENDING first, then earliest due date
    status_priority = case(
        (FollowUp.status == FollowUpStatus.PENDING, 1),
        (FollowUp.status == FollowUpStatus.COMPLETED, 2),
        (FollowUp.status == FollowUpStatus.CANCELLED, 3),
        else_=4
    )

    items = (
        query.order_by(status_priority, asc(FollowUp.due_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return FollowUpListResponse(
        items=items,
        total=total,
        page=page,
        page_size=page_size
    )


@router.post("", response_model=FollowUpResponse, status_code=status.HTTP_201_CREATED)
def create_follow_up(
    fu_in: FollowUpCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpResponse:
    """Create a new follow-up task."""
    # Determine assignee
    target_assignee_id = fu_in.assigned_user_id or current_user.id
    assigned_user = db.query(User).filter(
        User.id == target_assignee_id,
        User.clinic_id == current_user.clinic_id
    ).first()

    if not assigned_user or not assigned_user.is_active:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Assigned team member not found or inactive in your clinic."
        )

    # Validate Patient relationship
    if fu_in.patient_id:
        patient = db.query(Patient).filter(
            Patient.id == fu_in.patient_id,
            Patient.clinic_id == current_user.clinic_id
        ).first()
        if not patient:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Patient record not found in your clinic."
            )

    # Validate Lead relationship
    if fu_in.lead_id:
        lead = db.query(Lead).filter(
            Lead.id == fu_in.lead_id,
            Lead.clinic_id == current_user.clinic_id
        ).first()
        if not lead:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Lead record not found in your clinic."
            )

    follow_up = FollowUp(
        clinic_id=current_user.clinic_id,
        assigned_user_id=target_assignee_id,
        patient_id=fu_in.patient_id,
        lead_id=fu_in.lead_id,
        title=fu_in.title.strip(),
        notes=fu_in.notes.strip() if fu_in.notes else None,
        due_at=ensure_utc(fu_in.due_at),
        status=FollowUpStatus.PENDING,
        is_active=True
    )
    db.add(follow_up)
    db.commit()
    db.refresh(follow_up)
    return follow_up


@router.get("/{follow_up_id}", response_model=FollowUpResponse)
def get_follow_up(
    follow_up_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpResponse:
    """Get details for a specific follow-up task."""
    follow_up = db.query(FollowUp).options(
        joinedload(FollowUp.assigned_user),
        joinedload(FollowUp.patient),
        joinedload(FollowUp.lead)
    ).filter(
        FollowUp.id == follow_up_id,
        FollowUp.clinic_id == current_user.clinic_id
    ).first()

    if not follow_up:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Follow-up task not found."
        )
    return follow_up


@router.patch("/{follow_up_id}", response_model=FollowUpResponse)
def update_follow_up(
    follow_up_id: uuid.UUID,
    fu_in: FollowUpUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpResponse:
    """Reschedule or update a pending follow-up task."""
    follow_up = db.query(FollowUp).filter(
        FollowUp.id == follow_up_id,
        FollowUp.clinic_id == current_user.clinic_id
    ).first()

    if not follow_up:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Follow-up task not found."
        )

    if follow_up.status != FollowUpStatus.PENDING:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Completed or cancelled follow-up tasks cannot be modified."
        )

    if fu_in.assigned_user_id:
        assigned_user = db.query(User).filter(
            User.id == fu_in.assigned_user_id,
            User.clinic_id == current_user.clinic_id
        ).first()
        if not assigned_user or not assigned_user.is_active:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Assigned team member not found or inactive."
            )

    update_dict = fu_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        if field == "due_at" and value is not None:
            value = ensure_utc(value)
        setattr(follow_up, field, value)

    db.add(follow_up)
    db.commit()
    db.refresh(follow_up)
    return follow_up


@router.patch("/{follow_up_id}/complete", response_model=FollowUpResponse)
def complete_follow_up(
    follow_up_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpResponse:
    """Mark a follow-up task as COMPLETED."""
    follow_up = db.query(FollowUp).filter(
        FollowUp.id == follow_up_id,
        FollowUp.clinic_id == current_user.clinic_id
    ).first()

    if not follow_up:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Follow-up task not found."
        )

    if follow_up.status == FollowUpStatus.COMPLETED:
        return follow_up

    follow_up.status = FollowUpStatus.COMPLETED
    follow_up.completed_at = datetime.now(timezone.utc)
    db.add(follow_up)
    db.commit()
    db.refresh(follow_up)
    return follow_up


@router.patch("/{follow_up_id}/cancel", response_model=FollowUpResponse)
def cancel_follow_up(
    follow_up_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> FollowUpResponse:
    """Mark a follow-up task as CANCELLED."""
    follow_up = db.query(FollowUp).filter(
        FollowUp.id == follow_up_id,
        FollowUp.clinic_id == current_user.clinic_id
    ).first()

    if not follow_up:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Follow-up task not found."
        )

    if follow_up.status == FollowUpStatus.CANCELLED:
        return follow_up

    follow_up.status = FollowUpStatus.CANCELLED
    follow_up.cancelled_at = datetime.now(timezone.utc)
    db.add(follow_up)
    db.commit()
    db.refresh(follow_up)
    return follow_up
