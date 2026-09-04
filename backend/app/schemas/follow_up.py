import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict, field_validator, model_validator
from app.models.follow_up import FollowUpStatus
from app.schemas.appointment import PatientSummary
from app.schemas.user import UserResponse


class LeadSummary(BaseModel):
    id: uuid.UUID
    full_name: str
    phone: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class FollowUpBase(BaseModel):
    title: str
    due_at: datetime
    notes: Optional[str] = None
    assigned_user_id: Optional[uuid.UUID] = None
    patient_id: Optional[uuid.UUID] = None
    lead_id: Optional[uuid.UUID] = None


class FollowUpCreate(BaseModel):
    title: str
    due_at: datetime
    notes: Optional[str] = None
    assigned_user_id: Optional[uuid.UUID] = None
    patient_id: Optional[uuid.UUID] = None
    lead_id: Optional[uuid.UUID] = None

    @field_validator("title")
    @classmethod
    def validate_title(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Follow-up title cannot be blank.")
        return v.strip()

    @model_validator(mode="after")
    def validate_relationships(self) -> "FollowUpCreate":
        if self.patient_id is None and self.lead_id is None:
            raise ValueError("Follow-up must be associated with either a Patient or a Lead.")
        if self.patient_id is not None and self.lead_id is not None:
            raise ValueError("Follow-up cannot be associated with both a Patient and a Lead simultaneously.")
        return self


class FollowUpUpdate(BaseModel):
    title: Optional[str] = None
    due_at: Optional[datetime] = None
    notes: Optional[str] = None
    assigned_user_id: Optional[uuid.UUID] = None

    @field_validator("title")
    @classmethod
    def validate_title(cls, v: Optional[str]) -> Optional[str]:
        if v is not None and not v.strip():
            raise ValueError("Follow-up title cannot be blank.")
        return v.strip() if v else v


class FollowUpResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    assigned_user_id: uuid.UUID
    patient_id: Optional[uuid.UUID] = None
    lead_id: Optional[uuid.UUID] = None
    title: str
    notes: Optional[str] = None
    due_at: datetime
    status: FollowUpStatus
    is_overdue: bool
    completed_at: Optional[datetime] = None
    cancelled_at: Optional[datetime] = None
    assigned_user: Optional[UserResponse] = None
    patient: Optional[PatientSummary] = None
    lead: Optional[LeadSummary] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class FollowUpListResponse(BaseModel):
    items: List[FollowUpResponse]
    total: int
    page: int
    page_size: int
