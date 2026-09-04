import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict
from app.models.automation import DraftStatus, ExecutionStatus


class AutomationExecutionResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    event_type: str
    rule_type: str
    entity_type: str
    entity_id: uuid.UUID
    action_type: str
    status: ExecutionStatus
    idempotency_key: str
    executed_at: datetime
    error_message: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class AutomationExecutionListResponse(BaseModel):
    items: List[AutomationExecutionResponse]
    total: int
    page: int
    page_size: int


class CommunicationDraftResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    entity_type: str
    entity_id: uuid.UUID
    recipient_name: str
    recipient_phone: Optional[str] = None
    purpose: str
    message_content: str
    status: DraftStatus
    created_by_user_id: Optional[uuid.UUID] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class CommunicationDraftListResponse(BaseModel):
    items: List[CommunicationDraftResponse]
    total: int
    page: int
    page_size: int


class DraftStatusUpdate(BaseModel):
    status: DraftStatus
