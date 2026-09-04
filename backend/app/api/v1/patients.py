import uuid
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import or_, desc
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.patient import Patient
from app.models.patient_note import PatientNote
from app.models.tag import Tag
from app.models.user import User
from app.schemas.patient import (
    DuplicateCheckResponse,
    PatientCreate,
    PatientDetailResponse,
    PatientListResponse,
    PatientResponse,
    PatientStatusUpdate,
    PatientTagsUpdate,
    PatientUpdate,
)
from app.schemas.patient_note import PatientNoteCreate, PatientNoteResponse

router = APIRouter()


@router.get("", response_model=PatientListResponse)
def list_patients(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    search: Optional[str] = Query(None, description="Search by name or phone"),
    active_only: bool = Query(True, description="Filter active patients"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientListResponse:
    """List patients for the authenticated user's clinic with pagination and search."""
    query = db.query(Patient).filter(Patient.clinic_id == current_user.clinic_id)

    if active_only:
        query = query.filter(Patient.is_active.is_(True))

    if search and search.strip():
        search_term = f"%{search.strip()}%"
        query = query.filter(
            or_(
                Patient.full_name.ilike(search_term),
                Patient.phone.ilike(search_term)
            )
        )

    total = query.count()
    patients = (
        query.order_by(desc(Patient.created_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return PatientListResponse(
        items=patients,
        total=total,
        page=page,
        page_size=page_size
    )


@router.get("/check-duplicate", response_model=DuplicateCheckResponse)
def check_duplicate_patient(
    full_name: Optional[str] = Query(None),
    phone: Optional[str] = Query(None),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> DuplicateCheckResponse:
    """Check if potential matching patients exist in the clinic by phone or name."""
    if not full_name and not phone:
        return DuplicateCheckResponse(has_duplicates=False, possible_matches=[])

    conditions = []
    if phone and phone.strip():
        clean_phone = phone.strip()
        conditions.append(Patient.phone == clean_phone)
    if full_name and full_name.strip():
        clean_name = full_name.strip()
        conditions.append(Patient.full_name.ilike(clean_name))

    matches = db.query(Patient).filter(
        Patient.clinic_id == current_user.clinic_id,
        or_(*conditions)
    ).limit(5).all()

    return DuplicateCheckResponse(
        has_duplicates=len(matches) > 0,
        possible_matches=matches
    )


@router.post("", response_model=PatientResponse, status_code=status.HTTP_201_CREATED)
def create_patient(
    patient_in: PatientCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientResponse:
    """Create a new patient in the clinic."""
    patient = Patient(
        clinic_id=current_user.clinic_id,
        full_name=patient_in.full_name.strip(),
        phone=patient_in.phone.strip() if patient_in.phone else None,
        email=patient_in.email.strip() if patient_in.email else None,
        date_of_birth=patient_in.date_of_birth,
        gender=patient_in.gender,
        address=patient_in.address.strip() if patient_in.address else None,
        notes=patient_in.notes.strip() if patient_in.notes else None,
        is_active=True
    )

    if patient_in.tag_ids:
        tags = db.query(Tag).filter(
            Tag.id.in_(patient_in.tag_ids),
            Tag.clinic_id == current_user.clinic_id
        ).all()
        patient.tags = tags

    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.get("/{patient_id}", response_model=PatientDetailResponse)
def get_patient(
    patient_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientDetailResponse:
    """Get patient details including tags and notes for the authenticated user's clinic."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )
    return patient


@router.patch("/{patient_id}", response_model=PatientResponse)
def update_patient(
    patient_id: uuid.UUID,
    patient_in: PatientUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientResponse:
    """Update patient information."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )

    update_dict = patient_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        setattr(patient, field, value)

    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.patch("/{patient_id}/status", response_model=PatientResponse)
def update_patient_status(
    patient_id: uuid.UUID,
    status_in: PatientStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientResponse:
    """Soft archive or reactivate a patient."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )

    patient.is_active = status_in.is_active
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.put("/{patient_id}/tags", response_model=PatientResponse)
def update_patient_tags(
    patient_id: uuid.UUID,
    tags_in: PatientTagsUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientResponse:
    """Set tags for a patient."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )

    tags = db.query(Tag).filter(
        Tag.id.in_(tags_in.tag_ids),
        Tag.clinic_id == current_user.clinic_id
    ).all()

    patient.tags = tags
    db.add(patient)
    db.commit()
    db.refresh(patient)
    return patient


@router.get("/{patient_id}/notes", response_model=List[PatientNoteResponse])
def list_patient_notes(
    patient_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[PatientNoteResponse]:
    """List administrative notes for a patient."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )

    return db.query(PatientNote).filter(
        PatientNote.patient_id == patient_id,
        PatientNote.clinic_id == current_user.clinic_id
    ).order_by(desc(PatientNote.created_at)).all()


@router.post("/{patient_id}/notes", response_model=PatientNoteResponse, status_code=status.HTTP_201_CREATED)
def create_patient_note(
    patient_id: uuid.UUID,
    note_in: PatientNoteCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> PatientNoteResponse:
    """Add a new administrative note to a patient."""
    patient = db.query(Patient).filter(
        Patient.id == patient_id,
        Patient.clinic_id == current_user.clinic_id
    ).first()

    if not patient:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Patient not found."
        )

    note = PatientNote(
        clinic_id=current_user.clinic_id,
        patient_id=patient_id,
        author_user_id=current_user.id,
        content=note_in.content.strip()
    )
    db.add(note)
    db.commit()
    db.refresh(note)
    return note
