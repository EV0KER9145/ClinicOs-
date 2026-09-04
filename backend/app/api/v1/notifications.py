import uuid
from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc, func
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.dependencies.auth import get_current_user
from app.models.notification import Notification, NotificationType
from app.models.user import User
from app.schemas.notification import (
    NotificationListResponse,
    NotificationResponse,
    UnreadCountResponse,
)

router = APIRouter()


@router.get("", response_model=NotificationListResponse)
def list_notifications(
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=100),
    unread_only: bool = Query(False, description="Filter unread notifications only"),
    type_filter: Optional[NotificationType] = Query(None, alias="type", description="Filter by notification type"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> NotificationListResponse:
    """List notifications for the authenticated user (user & tenant isolated)."""
    base_query = db.query(Notification).filter(
        Notification.clinic_id == current_user.clinic_id,
        Notification.user_id == current_user.id
    )

    query = base_query
    if unread_only:
        query = query.filter(Notification.is_read.is_(False))

    if type_filter:
        query = query.filter(Notification.type == type_filter)

    total = query.count()
    unread_count = base_query.filter(Notification.is_read.is_(False)).count()

    items = (
        query.order_by(desc(Notification.created_at))
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    return NotificationListResponse(
        items=items,
        total=total,
        unread_count=unread_count,
        page=page,
        page_size=page_size
    )


@router.get("/unread-count", response_model=UnreadCountResponse)
def get_unread_count(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UnreadCountResponse:
    """Fast lightweight query for unread notification count badge."""
    count = db.query(func.count(Notification.id)).filter(
        Notification.clinic_id == current_user.clinic_id,
        Notification.user_id == current_user.id,
        Notification.is_read.is_(False)
    ).scalar() or 0

    return UnreadCountResponse(count=count)


@router.patch("/read-all", response_model=UnreadCountResponse)
def mark_all_notifications_read(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> UnreadCountResponse:
    """Mark all unread notifications for the authenticated user as read."""
    now_utc = datetime.now(timezone.utc)
    db.query(Notification).filter(
        Notification.clinic_id == current_user.clinic_id,
        Notification.user_id == current_user.id,
        Notification.is_read.is_(False)
    ).update(
        {
            Notification.is_read: True,
            Notification.read_at: now_utc
        },
        synchronize_session=False
    )
    db.commit()
    return UnreadCountResponse(count=0)


@router.patch("/{notification_id}/read", response_model=NotificationResponse)
def mark_notification_read(
    notification_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
) -> NotificationResponse:
    """Mark a specific notification as read."""
    notification = db.query(Notification).filter(
        Notification.id == notification_id,
        Notification.clinic_id == current_user.clinic_id,
        Notification.user_id == current_user.id
    ).first()

    if not notification:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Notification not found."
        )

    if not notification.is_read:
        notification.is_read = True
        notification.read_at = datetime.now(timezone.utc)
        db.add(notification)
        db.commit()
        db.refresh(notification)

    return notification
