from typing import Tuple
from fastapi import HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import SQLAlchemyError
from app.core.security import hash_password, verify_password, create_access_token
from app.models.clinic import Clinic
from app.models.user import User, UserRole
from app.schemas.auth import ClinicRegistrationRequest, LoginRequest, TokenResponse


def register_clinic_and_owner(db: Session, request: ClinicRegistrationRequest) -> Tuple[Clinic, User]:
    """Register a new clinic and its owner atomically."""
    # Check duplicate email
    existing_user = db.query(User).filter(User.email == request.email).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="An account with this email address already exists."
        )

    try:
        # Create Clinic
        clinic = Clinic(
            name=request.clinic_name,
            clinic_type=request.clinic_type,
            phone=request.clinic_phone,
            email=request.clinic_email
        )
        db.add(clinic)
        db.flush()  # Generates clinic.id

        # Create Owner User
        hashed_pwd = hash_password(request.password)
        owner = User(
            clinic_id=clinic.id,
            full_name=request.full_name,
            email=request.email,
            password_hash=hashed_pwd,
            role=UserRole.OWNER,
            is_active=True
        )
        db.add(owner)
        db.commit()
        db.refresh(clinic)
        db.refresh(owner)
        return clinic, owner
    except SQLAlchemyError as err:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to register clinic and owner due to a database error."
        ) from err


def authenticate_user(db: Session, request: LoginRequest) -> User:
    """Authenticate user with email and password."""
    user = db.query(User).filter(User.email == request.email).first()
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
            headers={"WWW-Authenticate": "Bearer"}
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is deactivated. Please contact support."
        )

    return user


def generate_user_token(user: User) -> TokenResponse:
    """Generate JWT bearer access token for authenticated user."""
    access_token = create_access_token(
        subject=str(user.id),
        clinic_id=str(user.clinic_id),
        role=user.role.value
    )
    return TokenResponse(access_token=access_token, token_type="bearer")
