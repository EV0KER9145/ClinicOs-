import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.doctor import Doctor
from app.models.user import User, UserRole


@pytest.fixture
def test_clinic_data(db_session: Session):
    """Fixture to create two distinct clinics and associated users/doctors."""
    # Clinic 1
    c1 = Clinic(name="Clinic One", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    c1_owner = User(
        clinic_id=c1.id,
        full_name="Dr. One Owner",
        email="c1_owner@clinic1.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    c1_staff = User(
        clinic_id=c1.id,
        full_name="Staff One",
        email="c1_staff@clinic1.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add_all([c1_owner, c1_staff])

    # Clinic 2
    c2 = Clinic(name="Clinic Two", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    c2_owner = User(
        clinic_id=c2.id,
        full_name="Dr. Two Owner",
        email="c2_owner@clinic2.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    db_session.add(c2_owner)

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(c1_owner)
    db_session.refresh(c1_staff)
    db_session.refresh(c2)
    db_session.refresh(c2_owner)

    return (c1, c1_owner, c1_staff), (c2, c2_owner)


def test_unauthenticated_doctors_access(client: TestClient):
    """Verify unauthenticated requests to doctors endpoints are rejected."""
    assert client.get("/api/v1/doctors").status_code == 401
    assert client.post("/api/v1/doctors", json={"full_name": "Dr. Test"}).status_code == 401


def test_owner_can_create_and_list_doctors(client: TestClient, test_clinic_data):
    """Verify clinic owner can create and list doctors."""
    (c1, c1_owner, _), _ = test_clinic_data
    token = create_access_token(subject=str(c1_owner.id), clinic_id=str(c1.id), role=c1_owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create doctor
    create_res = client.post(
        "/api/v1/doctors",
        json={"full_name": "Dr. Rahul Sharma", "specialty": "Dermatology"},
        headers=headers
    )
    assert create_res.status_code == 201
    doc_data = create_res.json()
    assert doc_data["full_name"] == "Dr. Rahul Sharma"
    assert doc_data["specialty"] == "Dermatology"
    assert doc_data["clinic_id"] == str(c1.id)

    # 2. List doctors
    list_res = client.get("/api/v1/doctors", headers=headers)
    assert list_res.status_code == 200
    docs = list_res.json()
    assert len(docs) == 1
    assert docs[0]["id"] == doc_data["id"]


def test_cross_tenant_doctor_isolation(client: TestClient, test_clinic_data, db_session: Session):
    """Verify doctors from Clinic 1 cannot be accessed or listed by Clinic 2."""
    (c1, c1_owner, _), (c2, c2_owner) = test_clinic_data

    # Create doctor in Clinic 1
    doc1 = Doctor(clinic_id=c1.id, full_name="Dr. Clinic One Specialist", specialty="Eye Care")
    db_session.add(doc1)
    db_session.commit()
    db_session.refresh(doc1)

    # Clinic 2 owner attempts to list doctors
    c2_token = create_access_token(subject=str(c2_owner.id), clinic_id=str(c2.id), role=c2_owner.role.value)
    c2_headers = {"Authorization": f"Bearer {c2_token}"}

    c2_list = client.get("/api/v1/doctors", headers=c2_headers).json()
    assert len(c2_list) == 0  # Does not see Clinic 1's doctor

    # Clinic 2 owner attempts to get Clinic 1's doctor directly
    c2_get = client.get(f"/api/v1/doctors/{doc1.id}", headers=c2_headers)
    assert c2_get.status_code == 404  # Safe 404


def test_doctor_update_and_deactivation(client: TestClient, test_clinic_data, db_session: Session):
    """Verify doctor updates and soft deactivation work properly."""
    (c1, c1_owner, _), _ = test_clinic_data
    token = create_access_token(subject=str(c1_owner.id), clinic_id=str(c1.id), role=c1_owner.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    doc = Doctor(clinic_id=c1.id, full_name="Dr. Old Name", specialty="General")
    db_session.add(doc)
    db_session.commit()
    db_session.refresh(doc)

    # Update doctor
    patch_res = client.patch(
        f"/api/v1/doctors/{doc.id}",
        json={"full_name": "Dr. New Name", "specialty": "Pediatrics"},
        headers=headers
    )
    assert patch_res.status_code == 200
    assert patch_res.json()["full_name"] == "Dr. New Name"

    # Soft deactivate doctor
    status_res = client.patch(
        f"/api/v1/doctors/{doc.id}/status",
        json={"is_active": False},
        headers=headers
    )
    assert status_res.status_code == 200
    assert status_res.json()["is_active"] is False

    # Soft deactivated doctor remains in database
    db_doc = db_session.query(Doctor).filter(Doctor.id == doc.id).first()
    assert db_doc is not None
    assert db_doc.is_active is False
