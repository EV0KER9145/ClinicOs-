import uuid
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, field_validator


class DoctorBase(BaseModel):
    full_name: str
    specialty: Optional[str] = None
    is_active: bool = True


class DoctorCreate(BaseModel):
    full_name: str
    specialty: Optional[str] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Doctor full name cannot be blank.")
        return v.strip()


class DoctorUpdate(BaseModel):
    full_name: Optional[str] = None
    specialty: Optional[str] = None
    is_active: Optional[bool] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("Doctor full name cannot be blank.")
        return v.strip() if v else v


class DoctorStatusUpdate(BaseModel):
    is_active: bool


class DoctorResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    user_id: Optional[uuid.UUID] = None
    full_name: str
    specialty: Optional[str] = None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
