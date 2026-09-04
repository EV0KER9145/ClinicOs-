import uuid
from typing import Optional
from sqlalchemy.orm import Session
from app.models.notification import Notification, NotificationType


class NotificationService:

    @staticmethod
    def create_notification(
        db: Session,
        clinic_id: uuid.UUID,
        user_id: uuid.UUID,
        title: str,
        message: str,
        type: NotificationType = NotificationType.GENERAL,
        entity_type: Optional[str] = None,
        entity_id: Optional[uuid.UUID] = None,
        action_route: Optional[str] = None
    ) -> Notification:
        """Create and persist a new in-app notification record."""
        notification = Notification(
            clinic_id=clinic_id,
            user_id=user_id,
            title=title.strip(),
            message=message.strip(),
            type=type,
            is_read=False,
            entity_type=entity_type,
            entity_id=entity_id,
            action_route=action_route
        )
        db.add(notification)
        db.flush()
        return notification
