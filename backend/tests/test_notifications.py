from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.notification import Notification, NotificationType
from app.models.user import User, UserRole
from app.services.notification_service import NotificationService


@pytest.fixture
def notif_multi_tenant_fixture(db_session: Session):
    """Fixture to create two clinics with multiple users for testing notifications."""
    # Clinic A
    c1 = Clinic(name="Notif Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a1 = User(
        clinic_id=c1.id,
        full_name="User A1",
        email="notif_a1@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    user_a2 = User(
        clinic_id=c1.id,
        full_name="User A2",
        email="notif_a2@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add_all([user_a1, user_a2])

    # Clinic B
    c2 = Clinic(name="Notif Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b1 = User(
        clinic_id=c2.id,
        full_name="User B1",
        email="notif_b1@clinicb.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    db_session.add(user_b1)

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a1)
    db_session.refresh(user_a2)
    db_session.refresh(c2)
    db_session.refresh(user_b1)

    return (c1, user_a1, user_a2), (c2, user_b1)


def test_unauthenticated_notification_access(client: TestClient):
    """Verify unauthenticated requests to notifications return 401."""
    assert client.get("/api/v1/notifications").status_code == 401
    assert client.get("/api/v1/notifications/unread-count").status_code == 401


def test_notification_creation_and_unread_count(client: TestClient, notif_multi_tenant_fixture, db_session: Session):
    """Verify creating notifications, unread count badge, and mark read functionality."""
    (c1, user_a1, _), _ = notif_multi_tenant_fixture

    # Create 2 notifications for User A1
    NotificationService.create_notification(
        db=db_session,
        clinic_id=c1.id,
        user_id=user_a1.id,
        title="Test Notification 1",
        message="Message 1",
        type=NotificationType.SYSTEM
    )
    n2 = NotificationService.create_notification(
        db=db_session,
        clinic_id=c1.id,
        user_id=user_a1.id,
        title="Test Notification 2",
        message="Message 2",
        type=NotificationType.FOLLOW_UP
    )
    db_session.commit()

    token = create_access_token(subject=str(user_a1.id), clinic_id=str(c1.id), role=user_a1.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # Unread count should be 2
    count_res = client.get("/api/v1/notifications/unread-count", headers=headers)
    assert count_res.status_code == 200
    assert count_res.json()["count"] == 2

    # Mark n2 as read
    read_res = client.patch(f"/api/v1/notifications/{n2.id}/read", headers=headers)
    assert read_res.status_code == 200
    assert read_res.json()["is_read"] is True

    # Unread count decrements to 1
    new_count = client.get("/api/v1/notifications/unread-count", headers=headers).json()
    assert new_count["count"] == 1


def test_mark_all_read(client: TestClient, notif_multi_tenant_fixture, db_session: Session):
    """Verify mark-all-read updates all unread notifications for current user."""
    (c1, user_a1, _), _ = notif_multi_tenant_fixture

    NotificationService.create_notification(db_session, c1.id, user_a1.id, "N1", "M1")
    NotificationService.create_notification(db_session, c1.id, user_a1.id, "N2", "M2")
    db_session.commit()

    token = create_access_token(subject=str(user_a1.id), clinic_id=str(c1.id), role=user_a1.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    read_all_res = client.patch("/api/v1/notifications/read-all", headers=headers)
    assert read_all_res.status_code == 200
    assert read_all_res.json()["count"] == 0

    count_after = client.get("/api/v1/notifications/unread-count", headers=headers).json()
    assert count_after["count"] == 0


def test_user_and_tenant_notification_isolation(client: TestClient, notif_multi_tenant_fixture, db_session: Session):
    """Verify User A1 cannot view or mark read User A2's or Clinic B's notifications."""
    (c1, user_a1, user_a2), (c2, user_b1) = notif_multi_tenant_fixture

    # Notification for User A2 (Clinic A)
    n_a2 = NotificationService.create_notification(db_session, c1.id, user_a2.id, "Secret A2", "Msg")
    # Notification for User B1 (Clinic B)
    n_b1 = NotificationService.create_notification(db_session, c2.id, user_b1.id, "Secret B1", "Msg")
    db_session.commit()

    # User A1 headers
    token_a1 = create_access_token(subject=str(user_a1.id), clinic_id=str(c1.id), role=user_a1.role.value)
    headers_a1 = {"Authorization": f"Bearer {token_a1}"}

    # User A1 list returns 0 notifications
    list_a1 = client.get("/api/v1/notifications", headers=headers_a1).json()
    assert list_a1["total"] == 0

    # User A1 attempting to mark A2's notification read returns 404
    assert client.patch(f"/api/v1/notifications/{n_a2.id}/read", headers=headers_a1).status_code == 404

    # User A1 attempting to mark B1's notification read returns 404
    assert client.patch(f"/api/v1/notifications/{n_b1.id}/read", headers=headers_a1).status_code == 404
