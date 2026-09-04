from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.clinic import Clinic
from app.models.user import User, UserRole
from app.schemas.clinic import ClinicResponse, ClinicUpdate

router = APIRouter()


@router.get("/me", response_model=ClinicResponse)
def get_my_clinic(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> ClinicResponse:
    """Retrieve the clinic associated with the authenticated user."""
    clinic = db.query(Clinic).filter(Clinic.id == current_user.clinic_id).first()
    if not clinic:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Clinic record not found."
        )
    return clinic


@router.patch("/me", response_model=ClinicResponse)
def update_my_clinic(
    update_data: ClinicUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> ClinicResponse:
    """Update profile information for the authenticated user's clinic (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to update clinic profile."
        )

    clinic = db.query(Clinic).filter(Clinic.id == current_user.clinic_id).first()
    if not clinic:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Clinic record not found."
        )

    update_dict = update_data.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        setattr(clinic, field, value)

    db.add(clinic)
    db.commit()
    db.refresh(clinic)
    return clinic
