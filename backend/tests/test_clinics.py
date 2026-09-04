import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.user import User, UserRole


@pytest.fixture
def test_clinic_and_users(db_session: Session):
    """Fixture to create a test clinic with an OWNER and a STAFF user."""
    clinic = Clinic(
        name="Apollo Care Clinic",
        clinic_type="General",
        phone="+919876543210",
        email="info@apollocare.com",
        timezone="Asia/Kolkata"
    )
    db_session.add(clinic)
    db_session.flush()

    owner = User(
        clinic_id=clinic.id,
        full_name="Dr. Owner Smith",
        email="owner@apollocare.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    staff = User(
        clinic_id=clinic.id,
        full_name="Staff Johnson",
        email="staff@apollocare.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add_all([owner, staff])
    db_session.commit()
    db_session.refresh(clinic)
    db_session.refresh(owner)
    db_session.refresh(staff)

    return clinic, owner, staff


def test_unauthenticated_clinic_access(client: TestClient):
    """Verify that unauthenticated requests to clinic endpoints return 401."""
    get_res = client.get("/api/v1/clinics/me")
    assert get_res.status_code == 401

    patch_res = client.patch("/api/v1/clinics/me", json={"name": "New Name"})
    assert patch_res.status_code == 401


def test_get_my_clinic(client: TestClient, test_clinic_and_users):
    """Verify authenticated user can retrieve their clinic profile."""
    clinic, owner, _ = test_clinic_and_users
    token = create_access_token(subject=str(owner.id), clinic_id=str(clinic.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    response = client.get("/api/v1/clinics/me", headers=headers)
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == str(clinic.id)
    assert data["name"] == "Apollo Care Clinic"
    assert data["email"] == "info@apollocare.com"


def test_owner_can_update_clinic_profile(client: TestClient, test_clinic_and_users):
    """Verify clinic OWNER can partially update clinic profile."""
    clinic, owner, _ = test_clinic_and_users
    token = create_access_token(subject=str(owner.id), clinic_id=str(clinic.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    update_payload = {
        "name": "Apollo Super Specialty Clinic",
        "phone": "+919999988888",
        "clinic_type": "Multi-Specialty"
    }

    response = client.patch("/api/v1/clinics/me", json=update_payload, headers=headers)
    assert response.status_code == 200
    data = response.json()
    assert data["name"] == "Apollo Super Specialty Clinic"
    assert data["phone"] == "+919999988888"
    assert data["clinic_type"] == "Multi-Specialty"
    # Unchanged fields remain preserved
    assert data["email"] == "info@apollocare.com"


def test_staff_cannot_update_clinic_profile(client: TestClient, test_clinic_and_users):
    """Verify STAFF role is forbidden from updating clinic profile."""
    clinic, _, staff = test_clinic_and_users
    token = create_access_token(subject=str(staff.id), clinic_id=str(clinic.id), role=staff.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    update_payload = {"name": "Staff Renamed Clinic"}

    response = client.patch("/api/v1/clinics/me", json=update_payload, headers=headers)
    assert response.status_code == 403
    assert "Owners or Admins" in response.json()["detail"]


def test_blank_clinic_name_validation(client: TestClient, test_clinic_and_users):
    """Verify blank clinic name is rejected by schema validation."""
    clinic, owner, _ = test_clinic_and_users
    token = create_access_token(subject=str(owner.id), clinic_id=str(clinic.id), role=owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    response = client.patch("/api/v1/clinics/me", json={"name": "   "}, headers=headers)
    assert response.status_code == 422
