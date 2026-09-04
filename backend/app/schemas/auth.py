import uuid
from typing import Optional
from pydantic import BaseModel, EmailStr, Field, ConfigDict
from app.models.user import UserRole
from app.schemas.clinic import ClinicResponse
from app.schemas.user import UserResponse


class ClinicRegistrationRequest(BaseModel):
    clinic_name: str = Field(..., min_length=2, max_length=255)
    clinic_type: Optional[str] = Field(None, max_length=100)
    clinic_phone: Optional[str] = Field(None, max_length=20)
    clinic_email: Optional[EmailStr] = None

    full_name: str = Field(..., min_length=2, max_length=255)
    email: EmailStr
    password: str = Field(..., min_length=8, max_length=128)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


class CurrentUserResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    full_name: str
    email: EmailStr
    role: UserRole

    model_config = ConfigDict(from_attributes=True)


class RegistrationResponse(BaseModel):
    clinic: ClinicResponse
    user: UserResponse
