import uuid
from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import or_, desc
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.lead import Lead, LeadSource, LeadStatus
from app.models.lead_note import LeadNote
from app.models.patient import Patient
from app.models.tag import Tag
from app.models.user import User
from app.schemas.lead import (
    LeadConvertRequest,
    LeadCreate,
    LeadDetailResponse,
    LeadListResponse,
    LeadResponse,
    LeadStatusUpdate,
    LeadUpdate,
)
from app.schemas.lead_note import LeadNoteCreate, LeadNoteResponse

router = APIRouter()


@router.get("", response_model=LeadListResponse)
def list_leads(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    search: Optional[str] = Query(None, description="Search by name, phone or email"),
    lead_status: Optional[LeadStatus] = Query(None, alias="status", description="Filter by status"),
    lead_source: Optional[LeadSource] = Query(None, alias="source", description="Filter by source"),
    assigned_user_id: Optional[uuid.UUID] = Query(None, description="Filter by assigned team member"),
    active_only: bool = Query(True, description="Filter active leads"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadListResponse:
    """List leads for the authenticated user's clinic with search, filtering and pagination."""
    query = db.query(Lead).filter(Lead.clinic_id == current_user.clinic_id)

    if active_only:
        query = query.filter(Lead.is_active.is_(True))

    if lead_status:
        query = query.filter(Lead.status == lead_status)

    if lead_source:
        query = query.filter(Lead.source == lead_source)

    if assigned_user_id:
        query = query.filter(Lead.assigned_user_id == assigned_user_id)

    if search and search.strip():
        term = f"%{search.strip()}%"
        query = query.filter(
            or_(
                Lead.full_name.ilike(term),
                Lead.phone.ilike(term),
                Lead.email.ilike(term)
            )
        )

    total = query.count()
    leads = (
        query.order_by(desc(Lead.created_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return LeadListResponse(
        items=leads,
        total=total,
        page=page,
        page_size=page_size
    )


@router.post("", response_model=LeadResponse, status_code=status.HTTP_201_CREATED)
def create_lead(
    lead_in: LeadCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadResponse:
    """Capture a new enquiry lead."""
    if lead_in.assigned_user_id:
        assigned_user = db.query(User).filter(
            User.id == lead_in.assigned_user_id,
            User.clinic_id == current_user.clinic_id
        ).first()
        if not assigned_user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Assigned team member not found in your clinic."
            )

    lead = Lead(
        clinic_id=current_user.clinic_id,
        full_name=lead_in.full_name.strip(),
        phone=lead_in.phone.strip() if lead_in.phone else None,
        email=lead_in.email.strip() if lead_in.email else None,
        source=lead_in.source,
        status=LeadStatus.NEW,
        assigned_user_id=lead_in.assigned_user_id,
        interested_service=lead_in.interested_service.strip() if lead_in.interested_service else None,
        notes=lead_in.notes.strip() if lead_in.notes else None,
        is_active=True
    )
    db.add(lead)
    db.commit()
    db.refresh(lead)
    return lead


@router.get("/{lead_id}", response_model=LeadDetailResponse)
def get_lead(
    lead_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadDetailResponse:
    """Get lead profile details including notes and conversion status."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )
    return lead


@router.patch("/{lead_id}", response_model=LeadResponse)
def update_lead(
    lead_id: uuid.UUID,
    lead_in: LeadUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadResponse:
    """Update lead information."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )

    if lead_in.assigned_user_id:
        assigned_user = db.query(User).filter(
            User.id == lead_in.assigned_user_id,
            User.clinic_id == current_user.clinic_id
        ).first()
        if not assigned_user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Assigned team member not found in your clinic."
            )

    update_dict = lead_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        setattr(lead, field, value)

    db.add(lead)
    db.commit()
    db.refresh(lead)
    return lead


@router.patch("/{lead_id}/status", response_model=LeadResponse)
def update_lead_status(
    lead_id: uuid.UUID,
    status_in: LeadStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadResponse:
    """Update lead status stage."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )

    if status_in.status == LeadStatus.CONVERTED:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="To convert a lead to a patient, please use the /convert endpoint."
        )

    lead.status = status_in.status
    db.add(lead)
    db.commit()
    db.refresh(lead)
    return lead


@router.post("/{lead_id}/convert", response_model=LeadResponse)
def convert_lead_to_patient(
    lead_id: uuid.UUID,
    convert_in: LeadConvertRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadResponse:
    """Convert an enquiry lead into a Patient atomically."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )

    if lead.status == LeadStatus.CONVERTED or lead.converted_patient_id is not None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This lead has already been converted to a patient."
        )

    target_patient: Optional[Patient] = None

    if convert_in.existing_patient_id:
        target_patient = db.query(Patient).filter(
            Patient.id == convert_in.existing_patient_id,
            Patient.clinic_id == current_user.clinic_id
        ).first()

        if not target_patient:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Target existing patient not found in your clinic."
            )
    elif convert_in.new_patient:
        np = convert_in.new_patient
        target_patient = Patient(
            clinic_id=current_user.clinic_id,
            full_name=np.full_name.strip(),
            phone=np.phone.strip() if np.phone else lead.phone,
            email=np.email.strip() if np.email else lead.email,
            date_of_birth=np.date_of_birth,
            gender=np.gender,
            address=np.address.strip() if np.address else None,
            notes=np.notes.strip() if np.notes else lead.notes,
            is_active=True
        )

        if np.tag_ids:
            tags = db.query(Tag).filter(
                Tag.id.in_(np.tag_ids),
                Tag.clinic_id == current_user.clinic_id
            ).all()
            target_patient.tags = tags

        db.add(target_patient)
        db.flush()
    else:
        # Fallback: create patient directly from Lead fields
        target_patient = Patient(
            clinic_id=current_user.clinic_id,
            full_name=lead.full_name,
            phone=lead.phone,
            email=lead.email,
            notes=lead.notes,
            is_active=True
        )
        db.add(target_patient)
        db.flush()

    # Link conversion atomically
    lead.converted_patient_id = target_patient.id
    lead.converted_at = datetime.now(timezone.utc)
    lead.status = LeadStatus.CONVERTED

    db.add(lead)
    db.commit()
    db.refresh(lead)
    return lead


@router.get("/{lead_id}/notes", response_model=List[LeadNoteResponse])
def list_lead_notes(
    lead_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[LeadNoteResponse]:
    """List administrative notes for a lead."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )

    return db.query(LeadNote).filter(
        LeadNote.lead_id == lead_id,
        LeadNote.clinic_id == current_user.clinic_id
    ).order_by(desc(LeadNote.created_at)).all()


@router.post("/{lead_id}/notes", response_model=LeadNoteResponse, status_code=status.HTTP_201_CREATED)
def create_lead_note(
    lead_id: uuid.UUID,
    note_in: LeadNoteCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> LeadNoteResponse:
    """Add a new administrative note to a lead."""
    lead = db.query(Lead).filter(
        Lead.id == lead_id,
        Lead.clinic_id == current_user.clinic_id
    ).first()

    if not lead:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Lead not found."
        )

    note = LeadNote(
        clinic_id=current_user.clinic_id,
        lead_id=lead_id,
        author_user_id=current_user.id,
        content=note_in.content.strip()
    )
    db.add(note)
    db.commit()
    db.refresh(note)
    return note
