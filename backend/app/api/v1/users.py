import uuid
from typing import List
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import hash_password
from app.dependencies.auth import get_current_user
from app.models.user import User, UserRole
from app.schemas.user import UserCreate, UserResponse, UserStatusUpdate, UserUpdate

router = APIRouter()


@router.get("", response_model=List[UserResponse])
def list_users(
    active_only: bool = Query(False, description="Filter active staff users only"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> List[UserResponse]:
    """List all staff users belonging to the authenticated user's clinic (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to view team members."
        )

    query = db.query(User).filter(User.clinic_id == current_user.clinic_id)
    if active_only:
        query = query.filter(User.is_active.is_(True))
    return query.order_by(User.created_at.asc()).all()


@router.post("", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def create_user(
    user_in: UserCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UserResponse:
    """Create a new staff user in the clinic (OWNER or ADMIN only)."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to add team members."
        )

    # Protect OWNER role creation
    if user_in.role == UserRole.OWNER:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Owner accounts cannot be created via team management."
        )

    # Check duplicate email
    existing_user = db.query(User).filter(User.email == user_in.email.lower().strip()).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email address already exists."
        )

    user = User(
        clinic_id=current_user.clinic_id,
        full_name=user_in.full_name.strip(),
        email=user_in.email.lower().strip(),
        phone=user_in.phone.strip() if user_in.phone else None,
        password_hash=hash_password(user_in.password),
        role=user_in.role,
        is_active=True
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.get("/{user_id}", response_model=UserResponse)
def get_user(
    user_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UserResponse:
    """Get a specific staff user by ID within the clinic."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to view team member details."
        )

    user = db.query(User).filter(
        User.id == user_id,
        User.clinic_id == current_user.clinic_id
    ).first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Team member not found."
        )
    return user


@router.patch("/{user_id}", response_model=UserResponse)
def update_user(
    user_id: uuid.UUID,
    user_in: UserUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UserResponse:
    """Update a staff user's details."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to update team members."
        )

    user = db.query(User).filter(
        User.id == user_id,
        User.clinic_id == current_user.clinic_id
    ).first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Team member not found."
        )

    # Protect against promoting to OWNER or editing an OWNER if ADMIN
    if user_in.role == UserRole.OWNER or (user.role == UserRole.OWNER and current_user.role == UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admins cannot modify Owner roles or accounts."
        )

    update_dict = user_in.model_dump(exclude_unset=True)
    for field, value in update_dict.items():
        setattr(user, field, value)

    db.add(user)
    db.commit()
    db.refresh(user)
    return user


@router.patch("/{user_id}/status", response_model=UserResponse)
def update_user_status(
    user_id: uuid.UUID,
    status_in: UserStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UserResponse:
    """Soft activate or deactivate a staff user."""
    if current_user.role not in (UserRole.OWNER, UserRole.ADMIN):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only clinic Owners or Admins are authorized to change team member status."
        )

    user = db.query(User).filter(
        User.id == user_id,
        User.clinic_id == current_user.clinic_id
    ).first()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Team member not found."
        )

    # Self-deactivation protection
    if user.id == current_user.id and not status_in.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You cannot deactivate your own account."
        )

    # Protect OWNER from being deactivated by ADMIN
    if user.role == UserRole.OWNER and current_user.role == UserRole.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admins cannot deactivate Owner accounts."
        )

    user.is_active = status_in.is_active
    db.add(user)
    db.commit()
    db.refresh(user)
    return user
