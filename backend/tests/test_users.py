import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.user import User, UserRole


@pytest.fixture
def test_users_fixture(db_session: Session):
    """Fixture to create test clinic, owner, admin, and staff users."""
    c1 = Clinic(name="User Test Clinic", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    owner = User(
        clinic_id=c1.id,
        full_name="Owner User",
        email="owner_test@clinic.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    admin = User(
        clinic_id=c1.id,
        full_name="Admin User",
        email="admin_test@clinic.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.ADMIN
    )
    staff = User(
        clinic_id=c1.id,
        full_name="Staff User",
        email="staff_test@clinic.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add_all([owner, admin, staff])
    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(owner)
    db_session.refresh(admin)
    db_session.refresh(staff)

    return c1, owner, admin, staff


def test_owner_can_create_staff_and_password_not_exposed(client: TestClient, test_users_fixture):
    """Verify OWNER can create STAFF user and password hash is never exposed in response."""
    c1, owner, _, _ = test_users_fixture
    token = create_access_token(subject=str(owner.id), clinic_id=str(c1.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    user_payload = {
        "full_name": "Priya Receptionist",
        "email": "priya@clinic.com",
        "password": "Password123!",
        "role": "STAFF",
        "phone": "+919876500000"
    }

    response = client.post("/api/v1/users", json=user_payload, headers=headers)
    assert response.status_code == 201
    data = response.json()
    assert data["full_name"] == "Priya Receptionist"
    assert data["email"] == "priya@clinic.com"
    assert data["role"] == "STAFF"
    assert "password" not in data
    assert "password_hash" not in data


def test_cannot_create_owner_user_via_team_management(client: TestClient, test_users_fixture):
    """Verify OWNER role creation via team management endpoint is rejected."""
    c1, owner, _, _ = test_users_fixture
    token = create_access_token(subject=str(owner.id), clinic_id=str(c1.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    user_payload = {
        "full_name": "Fake Owner",
        "email": "fakeowner@clinic.com",
        "password": "Password123!",
        "role": "OWNER"
    }

    response = client.post("/api/v1/users", json=user_payload, headers=headers)
    assert response.status_code == 400
    assert "Owner accounts cannot be created" in response.json()["detail"]


def test_staff_cannot_list_or_create_users(client: TestClient, test_users_fixture):
    """Verify STAFF role cannot list or create users."""
    c1, _, _, staff = test_users_fixture
    token = create_access_token(subject=str(staff.id), clinic_id=str(c1.id), role=staff.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    assert client.get("/api/v1/users", headers=headers).status_code == 403
    assert client.post("/api/v1/users", json={"full_name": "Test", "email": "t@c.com", "password": "Password123!"}, headers=headers).status_code == 403


def test_owner_cannot_deactivate_self(client: TestClient, test_users_fixture):
    """Verify OWNER cannot deactivate their own account."""
    c1, owner, _, _ = test_users_fixture
    token = create_access_token(subject=str(owner.id), clinic_id=str(c1.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    response = client.patch(f"/api/v1/users/{owner.id}/status", json={"is_active": False}, headers=headers)
    assert response.status_code == 400
    assert "deactivate your own account" in response.json()["detail"]
