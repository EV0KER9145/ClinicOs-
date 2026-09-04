import uuid
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict, field_validator
from app.schemas.patient_note import AuthorResponse


class LeadNoteCreate(BaseModel):
    content: str

    @field_validator("content")
    @classmethod
    def validate_content(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Note content cannot be blank.")
        return v.strip()


class LeadNoteResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    lead_id: uuid.UUID
    author_user_id: uuid.UUID
    author: Optional[AuthorResponse] = None
    content: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
