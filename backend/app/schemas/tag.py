import uuid
from datetime import datetime
from pydantic import BaseModel, ConfigDict, field_validator


class TagBase(BaseModel):
    name: str


class TagCreate(TagBase):
    @field_validator("name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Tag name cannot be blank.")
        return v.strip()


class TagResponse(TagBase):
    id: uuid.UUID
    clinic_id: uuid.UUID
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
