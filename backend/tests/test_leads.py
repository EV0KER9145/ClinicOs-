import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.lead import Lead, LeadSource, LeadStatus
from app.models.patient import Patient
from app.models.user import User, UserRole


@pytest.fixture
def leads_multi_tenant_fixture(db_session: Session):
    """Fixture to create two distinct clinics and staff users for testing leads."""
    c1 = Clinic(name="Lead Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="User A",
        email="lead_user_a@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    db_session.add(user_a)

    c2 = Clinic(name="Lead Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="User B",
        email="lead_user_b@clinicb.com",
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


def test_lead_crud_and_status_protection(client: TestClient, leads_multi_tenant_fixture):
    """Verify lead creation, status updates, filtering, and CONVERTED direct status protection."""
    (c1, user_a), _ = leads_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create Lead
    lead_payload = {
        "full_name": "Amit Shah",
        "phone": "+91 9876500111",
        "email": "amit@example.com",
        "source": "WHATSAPP",
        "interested_service": "Teeth Whitening"
    }
    create_res = client.post("/api/v1/leads", json=lead_payload, headers=headers)
    assert create_res.status_code == 201
    l_json = create_res.json()
    assert l_json["full_name"] == "Amit Shah"
    assert l_json["source"] == "WHATSAPP"
    assert l_json["status"] == "NEW"
    lead_id = l_json["id"]

    # 2. Update Status to CONTACTED
    status_res = client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONTACTED"}, headers=headers)
    assert status_res.status_code == 200
    assert status_res.json()["status"] == "CONTACTED"

    # 3. Direct status change to CONVERTED is rejected
    bad_status = client.patch(f"/api/v1/leads/{lead_id}/status", json={"status": "CONVERTED"}, headers=headers)
    assert bad_status.status_code == 400
    assert "/convert endpoint" in bad_status.json()["detail"]

    # 4. Filter list by source
    filter_res = client.get("/api/v1/leads?source=WHATSAPP", headers=headers)
    assert filter_res.status_code == 200
    assert filter_res.json()["total"] == 1


def test_lead_conversion_to_new_patient(client: TestClient, leads_multi_tenant_fixture, db_session: Session):
    """Verify converting a lead to a new patient atomically updates status and links patient."""
    (c1, user_a), _ = leads_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    lead = Lead(clinic_id=c1.id, full_name="Neha Singh", phone="+91 9999900000", source=LeadSource.INSTAGRAM)
    db_session.add(lead)
    db_session.commit()
    db_session.refresh(lead)

    # Convert to new patient
    convert_payload = {
        "new_patient": {
            "full_name": "Neha Singh",
            "phone": "+91 9999900000",
            "address": "45 Park Street"
        }
    }

    convert_res = client.post(f"/api/v1/leads/{lead.id}/convert", json=convert_payload, headers=headers)
    assert convert_res.status_code == 200
    data = convert_res.json()
    assert data["status"] == "CONVERTED"
    assert data["converted_patient_id"] is not None
    assert data["converted_patient"]["full_name"] == "Neha Singh"

    # Prevent double conversion
    double_res = client.post(f"/api/v1/leads/{lead.id}/convert", json=convert_payload, headers=headers)
    assert double_res.status_code == 400
    assert "already been converted" in double_res.json()["detail"]


def test_lead_cross_tenant_isolation(client: TestClient, leads_multi_tenant_fixture, db_session: Session):
    """Verify Clinic A's lead cannot be accessed or converted by Clinic B."""
    (c1, user_a), (c2, user_b) = leads_multi_tenant_fixture

    lead_a = Lead(clinic_id=c1.id, full_name="Clinic A Enquiry", source=LeadSource.GOOGLE)
    db_session.add(lead_a)
    db_session.commit()
    db_session.refresh(lead_a)

    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # Clinic B GET returns 404
    assert client.get(f"/api/v1/leads/{lead_a.id}", headers=headers_b).status_code == 404

    # Clinic B CONVERT returns 404
    assert client.post(f"/api/v1/leads/{lead_a.id}/convert", json={}, headers=headers_b).status_code == 404
