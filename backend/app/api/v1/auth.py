from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.user import User
from app.schemas.auth import (
    ClinicRegistrationRequest,
    LoginRequest,
    TokenResponse,
    CurrentUserResponse,
    RegistrationResponse,
)
from app.schemas.clinic import ClinicResponse
from app.schemas.user import UserResponse
from app.services import auth_service

router = APIRouter()


@router.post(
    "/register",
    response_model=RegistrationResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register a new clinic and owner account"
)
def register(
    request: ClinicRegistrationRequest,
    db: Session = Depends(get_db)
) -> RegistrationResponse:
    clinic, owner = auth_service.register_clinic_and_owner(db, request)
    return RegistrationResponse(
        clinic=ClinicResponse.model_validate(clinic),
        user=UserResponse.model_validate(owner)
    )


@router.post(
    "/login",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Authenticate user and issue access token"
)
def login(
    request: LoginRequest,
    db: Session = Depends(get_db)
) -> TokenResponse:
    user = auth_service.authenticate_user(db, request)
    return auth_service.generate_user_token(user)


@router.get(
    "/me",
    response_model=CurrentUserResponse,
    status_code=status.HTTP_200_OK,
    summary="Get current authenticated user details"
)
def get_me(
    current_user: User = Depends(get_current_user)
) -> CurrentUserResponse:
    return CurrentUserResponse.model_validate(current_user)
