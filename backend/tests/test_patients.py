import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.patient import Patient
from app.models.tag import Tag
from app.models.user import User, UserRole


@pytest.fixture
def multi_tenant_fixture(db_session: Session):
    """Fixture to create two distinct clinics and staff users."""
    # Clinic A
    c1 = Clinic(name="Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="User A",
        email="user_a@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add(user_a)

    # Clinic B
    c2 = Clinic(name="Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="User B",
        email="user_b@clinicb.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add(user_b)

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(c2)
    db_session.refresh(user_b)

    return (c1, user_a), (c2, user_b)


def test_patient_crud_and_search(client: TestClient, multi_tenant_fixture):
    """Verify patient creation, search, retrieval, update, and archiving."""
    (c1, user_a), _ = multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create Patient
    patient_data = {
        "full_name": "Rahul Sharma",
        "phone": "+91 9876543210",
        "email": "rahul@example.com",
        "notes": "Prefers morning appointments"
    }
    create_res = client.post("/api/v1/patients", json=patient_data, headers=headers)
    assert create_res.status_code == 201
    p_json = create_res.json()
    assert p_json["full_name"] == "Rahul Sharma"
    assert p_json["phone"] == "+91 9876543210"
    p_id = p_json["id"]

    # 2. Search Patient by Phone
    search_res = client.get("/api/v1/patients?search=98765", headers=headers)
    assert search_res.status_code == 200
    items = search_res.json()["items"]
    assert len(items) == 1
    assert items[0]["id"] == p_id

    # 3. Update Patient
    update_res = client.patch(f"/api/v1/patients/{p_id}", json={"address": "123 MG Road"}, headers=headers)
    assert update_res.status_code == 200
    assert update_res.json()["address"] == "123 MG Road"

    # 4. Soft Archive Patient
    archive_res = client.patch(f"/api/v1/patients/{p_id}/status", json={"is_active": False}, headers=headers)
    assert archive_res.status_code == 200
    assert archive_res.json()["is_active"] is False

    # 5. Active list excludes archived patient
    active_res = client.get("/api/v1/patients?active_only=true", headers=headers)
    assert active_res.json()["total"] == 0


def test_strict_multi_tenant_isolation(client: TestClient, multi_tenant_fixture, db_session: Session):
    """Verify Clinic A's patient cannot be accessed, searched, or modified by Clinic B."""
    (c1, user_a), (c2, user_b) = multi_tenant_fixture

    # Create patient in Clinic A
    p1 = Patient(clinic_id=c1.id, full_name="Clinic A Patient", phone="+919000000001")
    db_session.add(p1)
    db_session.commit()
    db_session.refresh(p1)

    # User B (Clinic B) headers
    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # Clinic B search returns 0 items
    search_b = client.get("/api/v1/patients?search=Clinic", headers=headers_b).json()
    assert search_b["total"] == 0

    # Clinic B direct GET returns 404
    get_b = client.get(f"/api/v1/patients/{p1.id}", headers=headers_b)
    assert get_b.status_code == 404

    # Clinic B direct PATCH returns 404
    patch_b = client.patch(f"/api/v1/patients/{p1.id}", json={"full_name": "Hacked Name"}, headers=headers_b)
    assert patch_b.status_code == 404


def test_patient_tags_and_notes(client: TestClient, multi_tenant_fixture):
    """Verify tag creation, assignment to patient, and patient notes workflow."""
    (c1, user_a), _ = multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create Patient
    p_res = client.post("/api/v1/patients", json={"full_name": "VIP Patient"}, headers=headers)
    p_id = p_res.json()["id"]

    # 2. Create Tag
    tag_res = client.post("/api/v1/tags", json={"name": "VIP"}, headers=headers)
    assert tag_res.status_code == 201
    tag_id = tag_res.json()["id"]

    # 3. Assign Tag to Patient
    assign_res = client.put(f"/api/v1/patients/{p_id}/tags", json={"tag_ids": [tag_id]}, headers=headers)
    assert assign_res.status_code == 200
    assert len(assign_res.json()["tags"]) == 1
    assert assign_res.json()["tags"][0]["name"] == "VIP"

    # 4. Add Patient Note
    note_res = client.post(
        f"/api/v1/patients/{p_id}/notes",
        json={"content": "Patient prefers evening consultations."},
        headers=headers
    )
    assert note_res.status_code == 201
    n_json = note_res.json()
    assert n_json["content"] == "Patient prefers evening consultations."
    assert n_json["author"]["full_name"] == "User A"

    # 5. Get Patient Detail includes Notes
    detail_res = client.get(f"/api/v1/patients/{p_id}", headers=headers)
    assert detail_res.status_code == 200
    assert len(detail_res.json()["notes_list"]) == 1
