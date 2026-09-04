import uuid
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, EmailStr, field_validator
from app.models.user import UserRole


class UserBase(BaseModel):
    full_name: str
    email: EmailStr
    phone: Optional[str] = None
    role: UserRole = UserRole.STAFF
    is_active: bool = True


class UserCreate(BaseModel):
    full_name: str
    email: EmailStr
    password: str
    role: UserRole = UserRole.STAFF
    phone: Optional[str] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Full name cannot be blank.")
        return v.strip()

    @field_validator("password")
    @classmethod
    def validate_password(cls, v: str) -> str:
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters long.")
        return v


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    role: Optional[UserRole] = None
    is_active: Optional[bool] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("Full name cannot be blank.")
        return v.strip() if v else v


class UserStatusUpdate(BaseModel):
    is_active: bool


class UserResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    full_name: str
    email: str
    phone: Optional[str] = None
    role: UserRole
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
