from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.clinic import Clinic
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead
from app.models.patient import Patient
from app.models.user import User, UserRole


@pytest.fixture
def fu_multi_tenant_fixture(db_session: Session):
    """Fixture to create two clinics, users, patients, and leads."""
    c1 = Clinic(name="FollowUp Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="User A",
        email="fu_a@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    pat_a = Patient(clinic_id=c1.id, full_name="Patient Alpha", phone="+91 9111111111")
    lead_a = Lead(clinic_id=c1.id, full_name="Lead Alpha", phone="+91 9222222222")
    db_session.add_all([user_a, pat_a, lead_a])

    c2 = Clinic(name="FollowUp Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="User B",
        email="fu_b@clinicb.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    pat_b = Patient(clinic_id=c2.id, full_name="Patient Beta", phone="+91 9333333333")
    db_session.add_all([user_b, pat_b])

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(pat_a)
    db_session.refresh(lead_a)
    db_session.refresh(c2)
    db_session.refresh(user_b)
    db_session.refresh(pat_b)

    return (c1, user_a, pat_a, lead_a), (c2, user_b, pat_b)


def test_create_follow_up_relationship_validation(client: TestClient, fu_multi_tenant_fixture):
    """Verify follow-up creation requires EITHER patient_id OR lead_id."""
    (c1, user_a, pat_a, lead_a), _ = fu_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    due_future = (datetime.now(timezone.utc) + timedelta(days=1)).isoformat()

    # 1. Valid Patient Follow-up
    p_res = client.post(
        "/api/v1/follow-ups",
        json={"title": "Call patient for review", "patient_id": str(pat_a.id), "due_at": due_future},
        headers=headers
    )
    assert p_res.status_code == 201
    assert p_res.json()["patient"]["full_name"] == "Patient Alpha"
    assert p_res.json()["is_overdue"] is False

    # 2. Valid Lead Follow-up
    l_res = client.post(
        "/api/v1/follow-ups",
        json={"title": "Follow up consultation enquiry", "lead_id": str(lead_a.id), "due_at": due_future},
        headers=headers
    )
    assert l_res.status_code == 201
    assert l_res.json()["lead"]["full_name"] == "Lead Alpha"

    # 3. Both relationships set -> Rejected
    both_res = client.post(
        "/api/v1/follow-ups",
        json={"title": "Invalid task", "patient_id": str(pat_a.id), "lead_id": str(lead_a.id), "due_at": due_future},
        headers=headers
    )
    assert both_res.status_code == 422

    # 4. Neither set -> Rejected
    neither_res = client.post(
        "/api/v1/follow-ups",
        json={"title": "Invalid task", "due_at": due_future},
        headers=headers
    )
    assert neither_res.status_code == 422


def test_dynamic_overdue_calculation_and_lifecycle(client: TestClient, fu_multi_tenant_fixture):
    """Verify overdue calculation and complete/cancel lifecycle transitions."""
    (c1, user_a, pat_a, _), _ = fu_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    due_past = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()

    # Create past due task
    create_res = client.post(
        "/api/v1/follow-ups",
        json={"title": "Overdue callback", "patient_id": str(pat_a.id), "due_at": due_past},
        headers=headers
    )
    assert create_res.status_code == 201
    data = create_res.json()
    assert data["is_overdue"] is True
    fu_id = data["id"]

    # Filter list by overdue
    overdue_res = client.get("/api/v1/follow-ups?overdue_only=true", headers=headers)
    assert overdue_res.status_code == 200
    assert overdue_res.json()["total"] == 1

    # Mark complete -> is_overdue becomes false
    comp_res = client.patch(f"/api/v1/follow-ups/{fu_id}/complete", headers=headers)
    assert comp_res.status_code == 200
    comp_json = comp_res.json()
    assert comp_json["status"] == "COMPLETED"
    assert comp_json["is_overdue"] is False
    assert comp_json["completed_at"] is not None

    # Cannot edit completed task
    edit_res = client.patch(f"/api/v1/follow-ups/{fu_id}", json={"title": "New Title"}, headers=headers)
    assert edit_res.status_code == 400


def test_follow_up_cross_tenant_isolation(client: TestClient, fu_multi_tenant_fixture, db_session: Session):
    """Verify Clinic A follow-ups cannot be accessed or completed by Clinic B."""
    (c1, user_a, pat_a, _), (c2, user_b, _) = fu_multi_tenant_fixture

    fu = FollowUp(
        clinic_id=c1.id,
        assigned_user_id=user_a.id,
        patient_id=pat_a.id,
        title="Clinic A Task",
        due_at=datetime.now(timezone.utc) + timedelta(days=1),
        status=FollowUpStatus.PENDING
    )
    db_session.add(fu)
    db_session.commit()
    db_session.refresh(fu)

    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # Clinic B GET returns 404
    assert client.get(f"/api/v1/follow-ups/{fu.id}", headers=headers_b).status_code == 404

    # Clinic B Complete returns 404
    assert client.patch(f"/api/v1/follow-ups/{fu.id}/complete", headers=headers_b).status_code == 404
