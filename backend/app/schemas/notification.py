import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict
from app.models.notification import NotificationType


class NotificationCreate(BaseModel):
    user_id: uuid.UUID
    title: str
    message: str
    type: NotificationType = NotificationType.GENERAL
    entity_type: Optional[str] = None
    entity_id: Optional[uuid.UUID] = None
    action_route: Optional[str] = None


class NotificationResponse(BaseModel):
    id: uuid.UUID
    clinic_id: uuid.UUID
    user_id: uuid.UUID
    title: str
    message: str
    type: NotificationType
    is_read: bool
    read_at: Optional[datetime] = None
    entity_type: Optional[str] = None
    entity_id: Optional[uuid.UUID] = None
    action_route: Optional[str] = None
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class NotificationListResponse(BaseModel):
    items: List[NotificationResponse]
    total: int
    unread_count: int
    page: int
    page_size: int


class UnreadCountResponse(BaseModel):
    count: int
