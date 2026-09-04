from typing import List
from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.tag import Tag
from app.models.user import User
from app.schemas.tag import TagCreate, TagResponse

router = APIRouter()


@router.get("", response_model=List[TagResponse])
def list_tags(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[TagResponse]:
    """List all tags belonging to the authenticated user's clinic."""
    return db.query(Tag).filter(Tag.clinic_id == current_user.clinic_id).order_by(Tag.name.asc()).all()


@router.post("", response_model=TagResponse, status_code=status.HTTP_201_CREATED)
def create_tag(
    tag_in: TagCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> TagResponse:
    """Create a new tag for the clinic."""
    clean_name = tag_in.name.strip()
    existing_tag = db.query(Tag).filter(
        Tag.clinic_id == current_user.clinic_id,
        Tag.name.ilike(clean_name)
    ).first()

    if existing_tag:
        return existing_tag

    tag = Tag(
        clinic_id=current_user.clinic_id,
        name=clean_name
    )
    db.add(tag)
    db.commit()
    db.refresh(tag)
    return tag
