from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.appointment import Appointment, AppointmentStatus
from app.models.clinic import Clinic
from app.models.doctor import Doctor
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead, LeadSource
from app.models.patient import Patient
from app.models.user import User, UserRole


@pytest.fixture
def ai_test_fixture(db_session: Session):
    """Fixture to set up test clinic data for AI differentiation features."""
    c1 = Clinic(name="AI Test Clinic", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="Dr. AI Owner",
        email="ai_owner@clinic.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    doc_a = Doctor(clinic_id=c1.id, full_name="Dr. AI Doctor", specialty="General")
    pat_a = Patient(clinic_id=c1.id, full_name="Rahul Sharma", phone="+91 9876543210")
    lead_a = Lead(clinic_id=c1.id, full_name="Priya Lead", source=LeadSource.WHATSAPP, interested_service="Teeth Whitening")

    db_session.add_all([user_a, doc_a, pat_a, lead_a])
    db_session.flush()

    now_utc = datetime.now(timezone.utc)
    fu_overdue = FollowUp(
        clinic_id=c1.id,
        assigned_user_id=user_a.id,
        patient_id=pat_a.id,
        title="Call patient regarding review",
        due_at=now_utc - timedelta(days=1),
        status=FollowUpStatus.PENDING
    )
    appt_noshow = Appointment(
        clinic_id=c1.id,
        patient_id=pat_a.id,
        doctor_id=doc_a.id,
        scheduled_at=now_utc - timedelta(days=2),
        duration_minutes=30,
        status=AppointmentStatus.NO_SHOW
    )
    db_session.add_all([fu_overdue, appt_noshow])
    db_session.commit()

    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(pat_a)
    db_session.refresh(lead_a)

    return c1, user_a, pat_a, lead_a


def test_unauthenticated_ai_access(client: TestClient):
    """Verify unauthenticated requests to AI endpoints return 401."""
    assert client.post("/api/v1/ai/follow-up-recommendations").status_code == 401
    assert client.post("/api/v1/ai/generate-message", json={"entity_type": "PATIENT", "entity_id": "00000000-0000-0000-0000-000000000000", "purpose": "FOLLOW_UP"}).status_code == 401
    assert client.get("/api/v1/ai/clinic-insights").status_code == 401
    assert client.post("/api/v1/ai/analytics/query", json={"question": "test"}).status_code == 401


def test_c1_follow_up_recommendations(client: TestClient, ai_test_fixture):
    """Verify C1 AI Follow-up Intelligence generates prioritized action recommendations."""
    c1, user_a, _, _ = ai_test_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    res = client.post("/api/v1/ai/follow-up-recommendations", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["total_candidates"] > 0
    recs = data["recommendations"]
    assert len(recs) > 0
    # Overdue task recommendation present
    assert any(r["category"] == "OVERDUE_FOLLOW_UP" for r in recs)


def test_c2_message_assistant(client: TestClient, ai_test_fixture):
    """Verify C2 AI Message Assistant generates non-clinical administrative messages."""
    c1, user_a, pat_a, _ = ai_test_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    req_payload = {
        "entity_type": "PATIENT",
        "entity_id": str(pat_a.id),
        "purpose": "NO_SHOW_RECOVERY",
        "tone": "FRIENDLY"
    }

    res = client.post("/api/v1/ai/generate-message", json=req_payload, headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["recipient_name"] == "Rahul Sharma"
    assert "Rahul" in data["message"]
    assert data["purpose"] == "NO_SHOW_RECOVERY"


def test_c3_clinic_insights(client: TestClient, ai_test_fixture):
    """Verify C3 AI Clinic Insights produces actionable insights from clinic metrics."""
    c1, user_a, _, _ = ai_test_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    res = client.get("/api/v1/ai/clinic-insights", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["clinic_name"] == "AI Test Clinic"
    assert len(data["insights"]) > 0


def test_c4_natural_language_analytics_and_safety_boundary(client: TestClient, ai_test_fixture):
    """Verify C4 Natural Language Analytics executes deterministic queries and enforces clinical safety boundary."""
    c1, user_a, _, _ = ai_test_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Valid administrative query
    res_valid = client.post(
        "/api/v1/ai/analytics/query",
        json={"question": "How many follow-ups are overdue?"},
        headers=headers
    )
    assert res_valid.status_code == 200
    data_valid = res_valid.json()
    assert data_valid["intent"] == "OVERDUE_FOLLOW_UP_COUNT"
    assert "overdue" in data_valid["answer"].lower()

    # 2. Medical query rejected by Safety Boundary
    res_med = client.post(
        "/api/v1/ai/analytics/query",
        json={"question": "What medicine should I prescribe for diagnosis of skin rash?"},
        headers=headers
    )
    assert res_med.status_code == 200
    data_med = res_med.json()
    assert data_med["intent"] == "CLINICAL_PROHIBITED"
    assert "does not provide medical diagnoses" in data_med["answer"]
