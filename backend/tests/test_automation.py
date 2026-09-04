from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.automation.events import AutomationEvent, AutomationEventType
from app.automation.service import AutomationService
from app.core.security import create_access_token, hash_password
from app.models.appointment import Appointment, AppointmentStatus
from app.models.automation import AutomationExecution, CommunicationDraft, DraftStatus
from app.models.clinic import Clinic
from app.models.doctor import Doctor
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.patient import Patient
from app.models.user import User, UserRole


@pytest.fixture
def auto_multi_tenant_fixture(db_session: Session):
    """Fixture to set up test clinics, users, patients, doctors, and appointments for automation tests."""
    c1 = Clinic(name="Automation Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="User A",
        email="auto_a@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    doc_a = Doctor(clinic_id=c1.id, full_name="Dr. Alpha", specialty="General")
    pat_a = Patient(clinic_id=c1.id, full_name="Patient Alpha", phone="+91 9111111111")
    db_session.add_all([user_a, doc_a, pat_a])

    c2 = Clinic(name="Automation Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="User B",
        email="auto_b@clinicb.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.OWNER
    )
    db_session.add(user_b)

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(doc_a)
    db_session.refresh(pat_a)
    db_session.refresh(c2)
    db_session.refresh(user_b)

    return (c1, user_a, doc_a, pat_a), (c2, user_b)


def test_no_show_recovery_rule_and_idempotency(client: TestClient, auto_multi_tenant_fixture, db_session: Session):
    """Verify marking appointment as NO_SHOW triggers recovery follow-up + draft + notification idempotently."""
    (c1, user_a, doc_a, pat_a), _ = auto_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create Appointment
    now_utc = datetime.now(timezone.utc)
    appt = Appointment(
        clinic_id=c1.id,
        patient_id=pat_a.id,
        doctor_id=doc_a.id,
        scheduled_at=now_utc - timedelta(hours=2),
        duration_minutes=30,
        status=AppointmentStatus.SCHEDULED
    )
    db_session.add(appt)
    db_session.commit()
    db_session.refresh(appt)

    # 2. Update status to NO_SHOW via API
    status_res = client.patch(f"/api/v1/appointments/{appt.id}/status", json={"status": "NO_SHOW"}, headers=headers)
    assert status_res.status_code == 200
    assert status_res.json()["status"] == "NO_SHOW"

    # 3. Verify exactly 1 recovery follow-up created
    fu = db_session.query(FollowUp).filter(FollowUp.patient_id == pat_a.id).first()
    assert fu is not None
    assert "Reschedule Missed Appointment" in fu.title

    # 4. Verify 1 Communication Draft created
    draft = db_session.query(CommunicationDraft).filter(CommunicationDraft.entity_id == appt.id).first()
    assert draft is not None
    assert draft.purpose == "NO_SHOW_RECOVERY"
    assert "Patient Alpha" in draft.recipient_name

    # 5. Idempotency test: Triggering NO_SHOW recovery rule second time skips creation
    event = AutomationEvent(
        event_type=AutomationEventType.APPOINTMENT_NO_SHOW,
        clinic_id=c1.id,
        entity_type="APPOINTMENT",
        entity_id=appt.id
    )
    AutomationService.dispatch_event(db_session, event)

    fus_count = db_session.query(FollowUp).filter(FollowUp.patient_id == pat_a.id).count()
    assert fus_count == 1  # No duplicate follow-up created!


def test_time_driven_rules_evaluation(client: TestClient, auto_multi_tenant_fixture, db_session: Session):
    """Verify time-driven rules evaluation endpoint creates overdue alerts."""
    (c1, user_a, _, pat_a), _ = auto_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    now_utc = datetime.now(timezone.utc)
    # Overdue pending task
    fu = FollowUp(
        clinic_id=c1.id,
        assigned_user_id=user_a.id,
        patient_id=pat_a.id,
        title="Past Due Callback",
        due_at=now_utc - timedelta(hours=5),
        status=FollowUpStatus.PENDING
    )
    db_session.add(fu)
    db_session.commit()

    eval_res = client.post("/api/v1/automations/evaluate-time-rules", headers=headers)
    assert eval_res.status_code == 200
    assert eval_res.json()["rules_executed"] >= 1


def test_communication_drafts_api_and_status(client: TestClient, auto_multi_tenant_fixture, db_session: Session):
    """Verify listing communication drafts and updating draft status."""
    (c1, user_a, _, _), _ = auto_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    draft = CommunicationDraft(
        clinic_id=c1.id,
        entity_type="PATIENT",
        entity_id=user_a.id,
        recipient_name="Rahul",
        purpose="FOLLOW_UP",
        message_content="Hi Rahul, checking in.",
        status=DraftStatus.DRAFT
    )
    db_session.add(draft)
    db_session.commit()
    db_session.refresh(draft)

    # List Drafts
    list_res = client.get("/api/v1/automations/drafts", headers=headers)
    assert list_res.status_code == 200
    assert list_res.json()["total"] == 1

    # Approve Draft
    approve_res = client.patch(f"/api/v1/automations/drafts/{draft.id}/status", json={"status": "APPROVED"}, headers=headers)
    assert approve_res.status_code == 200
    assert approve_res.json()["status"] == "APPROVED"


def test_automation_multi_tenant_isolation(client: TestClient, auto_multi_tenant_fixture, db_session: Session):
    """Verify Clinic A communication draft cannot be accessed or updated by Clinic B."""
    (c1, user_a, _, _), (c2, user_b) = auto_multi_tenant_fixture

    draft_a = CommunicationDraft(
        clinic_id=c1.id,
        entity_type="LEAD",
        entity_id=user_a.id,
        recipient_name="Clinic A Lead",
        purpose="LEAD_REENGAGEMENT",
        message_content="Clinic A Message",
        status=DraftStatus.DRAFT
    )
    db_session.add(draft_a)
    db_session.commit()
    db_session.refresh(draft_a)

    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # Clinic B list returns 0 drafts
    list_b = client.get("/api/v1/automations/drafts", headers=headers_b).json()
    assert list_b["total"] == 0

    # Clinic B update returns 404
    patch_b = client.patch(f"/api/v1/automations/drafts/{draft_a.id}/status", json={"status": "APPROVED"}, headers=headers_b)
    assert patch_b.status_code == 404
