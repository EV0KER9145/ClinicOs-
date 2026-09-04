import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, EmailStr, field_validator
from app.models.lead import LeadSource, LeadStatus
from app.schemas.user import UserResponse
from app.schemas.patient import PatientResponse, PatientCreate
from app.schemas.lead_note import LeadNoteResponse


class LeadBase(BaseModel):
    full_name: str
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    source: LeadSource = LeadSource.PHONE
    status: LeadStatus = LeadStatus.NEW
    assigned_user_id: Optional[uuid.UUID] = None
    interested_service: Optional[str] = None
    notes: Optional[str] = None


class LeadCreate(BaseModel):
    full_name: str
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    source: LeadSource = LeadSource.PHONE
    assigned_user_id: Optional[uuid.UUID] = None
    interested_service: Optional[str] = None
    notes: Optional[str] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Lead full name cannot be blank.")
        return v.strip()


class LeadUpdate(BaseModel):
    full_name: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[EmailStr] = None
    source: Optional[LeadSource] = None
    assigned_user_id: Optional[uuid.UUID] = None
    interested_service: Optional[str] = None
    notes: Optional[str] = None
    is_active: Optional[bool] = None

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("Lead full name cannot be blank.")
        return v.strip() if v else v


class LeadStatusUpdate(BaseModel):
    status: LeadStatus


class LeadConvertRequest(BaseModel):
    existing_patient_id: Optional[uuid.UUID] = None
    new_patient: Optional[PatientCreate] = None


class LeadResponse(LeadBase):
    id: uuid.UUID
    clinic_id: uuid.UUID
    is_active: bool
    converted_patient_id: Optional[uuid.UUID] = None
    converted_at: Optional[datetime] = None
    assigned_user: Optional[UserResponse] = None
    converted_patient: Optional[PatientResponse] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class LeadDetailResponse(LeadResponse):
    notes_list: List[LeadNoteResponse] = []

    model_config = ConfigDict(from_attributes=True)


class LeadListResponse(BaseModel):
    items: List[LeadResponse]
    total: int
    page: int
    page_size: int
