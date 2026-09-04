import uuid
from datetime import date, datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, EmailStr, field_validator
from app.schemas.tag import TagResponse
from app.schemas.patient_note import PatientNoteResponse


class PatientBase(BaseModel):
    full_name: str
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    date_of_birth: Optional[date] = None
    gender: Optional[str] = None
    address: Optional[str] = None
    notes: Optional[str] = None
    is_active: bool = True


class PatientCreate(BaseModel):
    full_name: str
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    date_of_birth: Optional[date] = None
    gender: Optional[str] = None
    address: Optional[str] = None
    notes: Optional[str] = None
    tag_ids: Optional[List[uuid.UUID]] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Patient full name cannot be blank.")
        return v.strip()


class PatientUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    date_of_birth: Optional[date] = None
    gender: Optional[str] = None
    address: Optional[str] = None
    notes: Optional[str] = None
    is_active: Optional[bool] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("Patient full name cannot be blank.")
        return v.strip() if v else v


class PatientStatusUpdate(BaseModel):
    is_active: bool


class PatientTagsUpdate(BaseModel):
    tag_ids: List[uuid.UUID]


class PatientResponse(PatientBase):
    id: uuid.UUID
    clinic_id: uuid.UUID
    tags: List[TagResponse] = []
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class PatientDetailResponse(PatientResponse):
    notes_list: List[PatientNoteResponse] = []

    model_config = ConfigDict(from_attributes=True)


class PatientListResponse(BaseModel):
    items: List[PatientResponse]
    total: int
    page: int
    page_size: int


class DuplicateCheckResponse(BaseModel):
    has_duplicates: bool
    possible_matches: List[PatientResponse] = []
