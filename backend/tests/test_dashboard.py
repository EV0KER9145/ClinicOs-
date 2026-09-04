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
def dashboard_multi_tenant_fixture(db_session: Session):
    """Fixture to create two distinct clinics and data for dashboard testing."""
    c1 = Clinic(name="Apex Care Clinic", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="Dr. Apex Owner",
        email="owner_dash@apex.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    doc_a = Doctor(clinic_id=c1.id, full_name="Dr. Apex Doctor", specialty="General")
    pat_a = Patient(clinic_id=c1.id, full_name="Apex Patient", phone="+91 9000011111")
    lead_a = Lead(clinic_id=c1.id, full_name="Apex Lead", source=LeadSource.WHATSAPP)
    db_session.add_all([user_a, doc_a, pat_a, lead_a])

    # Clinic B
    c2 = Clinic(name="Beta Health Clinic", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="Dr. Beta Owner",
        email="owner_dash@beta.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    db_session.add(user_b)

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(doc_a)
    db_session.refresh(pat_a)
    db_session.refresh(lead_a)
    db_session.refresh(c2)
    db_session.refresh(user_b)

    return (c1, user_a, doc_a, pat_a, lead_a), (c2, user_b)


def test_unauthenticated_dashboard_access(client: TestClient):
    """Verify unauthenticated requests to dashboard return 401."""
    assert client.get("/api/v1/dashboard/summary").status_code == 401


def test_empty_clinic_dashboard_summary(client: TestClient, dashboard_multi_tenant_fixture):
    """Verify a new/empty clinic receives zero metrics and empty lists without errors."""
    _, (_, user_b) = dashboard_multi_tenant_fixture
    token = create_access_token(subject=str(user_b.id), clinic_id=str(user_b.clinic_id), role=user_b.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    res = client.get("/api/v1/dashboard/summary", headers=headers)
    assert res.status_code == 200
    data = res.json()
    assert data["clinic_name"] == "Beta Health Clinic"
    assert data["today"]["appointments_count"] == 0
    assert data["today"]["overdue_follow_ups_count"] == 0
    assert data["attention"]["overdue_follow_ups"] == 0
    assert data["upcoming_appointments"] == []
    assert data["recent_no_shows"] == []


def test_dashboard_metrics_and_multi_tenant_isolation(client: TestClient, dashboard_multi_tenant_fixture, db_session: Session):
    """Verify dashboard metrics, overdue calculations, and strict tenant isolation."""
    (c1, user_a, doc_a, pat_a, _), (c2, user_b) = dashboard_multi_tenant_fixture

    now_utc = datetime.now(timezone.utc)

    # 1. Add overdue follow-up for Clinic A
    fu_overdue = FollowUp(
        clinic_id=c1.id,
        assigned_user_id=user_a.id,
        patient_id=pat_a.id,
        title="Overdue Callback",
        due_at=now_utc - timedelta(days=1),
        status=FollowUpStatus.PENDING
    )
    # 2. Add upcoming appointment for Clinic A
    appt_upcoming = Appointment(
        clinic_id=c1.id,
        patient_id=pat_a.id,
        doctor_id=doc_a.id,
        scheduled_at=now_utc + timedelta(hours=2),
        duration_minutes=30,
        status=AppointmentStatus.SCHEDULED
    )
    # 3. Add no-show appointment for Clinic A
    appt_noshow = Appointment(
        clinic_id=c1.id,
        patient_id=pat_a.id,
        doctor_id=doc_a.id,
        scheduled_at=now_utc - timedelta(hours=3),
        duration_minutes=30,
        status=AppointmentStatus.NO_SHOW
    )
    db_session.add_all([fu_overdue, appt_upcoming, appt_noshow])
    db_session.commit()

    # Query Clinic A Dashboard
    token_a = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers_a = {"Authorization": f"Bearer {token_a}"}

    res_a = client.get("/api/v1/dashboard/summary", headers=headers_a)
    assert res_a.status_code == 200
    data_a = res_a.json()
    assert data_a["attention"]["overdue_follow_ups"] == 1
    assert data_a["today"]["no_shows_count"] == 1
    assert len(data_a["upcoming_appointments"]) == 1
    assert data_a["upcoming_appointments"][0]["patient_name"] == "Apex Patient"
    assert len(data_a["recent_no_shows"]) == 1

    # Query Clinic B Dashboard -> Strict zero metrics (Tenant Isolation)
    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    res_b = client.get("/api/v1/dashboard/summary", headers=headers_b)
    assert res_b.status_code == 200
    data_b = res_b.json()
    assert data_b["attention"]["overdue_follow_ups"] == 0
    assert data_b["today"]["no_shows_count"] == 0
    assert len(data_b["upcoming_appointments"]) == 0
    assert len(data_b["recent_no_shows"]) == 0
