import uuid
from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict


class DoctorBase(BaseModel):
    full_name: str
    specialty: Optional[str] = None
    is_active: bool = True


class DoctorCreate(DoctorBase):
    clinic_id: uuid.UUID
    user_id: Optional[uuid.UUID] = None


class DoctorUpdate(BaseModel):
    full_name: Optional[str] = None
    specialty: Optional[str] = None
    user_id: Optional[uuid.UUID] = None
    is_active: Optional[bool] = None


class DoctorResponse(DoctorBase):
    id: uuid.UUID
    clinic_id: uuid.UUID
    user_id: Optional[uuid.UUID] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)
