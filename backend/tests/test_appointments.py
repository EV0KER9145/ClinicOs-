from datetime import datetime, timedelta, timezone
import pytest
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session
from app.core.security import create_access_token, hash_password
from app.models.appointment import Appointment, AppointmentStatus
from app.models.clinic import Clinic
from app.models.doctor import Doctor
from app.models.patient import Patient
from app.models.user import User, UserRole


@pytest.fixture
def appt_multi_tenant_fixture(db_session: Session):
    """Fixture to create two distinct clinics, doctors, patients, and staff users."""
    # Clinic A
    c1 = Clinic(name="Appt Clinic A", timezone="Asia/Kolkata")
    db_session.add(c1)
    db_session.flush()

    user_a = User(
        clinic_id=c1.id,
        full_name="User A",
        email="appt_a@clinica.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    doc_a = Doctor(clinic_id=c1.id, full_name="Dr. Alpha", specialty="General")
    pat_a = Patient(clinic_id=c1.id, full_name="Patient Alpha", phone="+91 9111111111")
    db_session.add_all([user_a, doc_a, pat_a])

    # Clinic B
    c2 = Clinic(name="Appt Clinic B", timezone="Asia/Kolkata")
    db_session.add(c2)
    db_session.flush()

    user_b = User(
        clinic_id=c2.id,
        full_name="User B",
        email="appt_b@clinicb.com",
        password_hash=hash_password("Secret123!"),
        role=UserRole.STAFF
    )
    doc_b = Doctor(clinic_id=c2.id, full_name="Dr. Beta", specialty="Dentistry")
    pat_b = Patient(clinic_id=c2.id, full_name="Patient Beta", phone="+91 9222222222")
    db_session.add_all([user_b, doc_b, pat_b])

    db_session.commit()
    db_session.refresh(c1)
    db_session.refresh(user_a)
    db_session.refresh(doc_a)
    db_session.refresh(pat_a)
    db_session.refresh(c2)
    db_session.refresh(user_b)
    db_session.refresh(doc_b)
    db_session.refresh(pat_b)

    return (c1, user_a, doc_a, pat_a), (c2, user_b, doc_b, pat_b)


def test_create_and_list_appointment(client: TestClient, appt_multi_tenant_fixture):
    """Verify booking a valid appointment and listing appointments by date."""
    (c1, user_a, doc_a, pat_a), _ = appt_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    now_utc = datetime.now(timezone.utc).replace(microsecond=0) + timedelta(days=1)
    appt_payload = {
        "patient_id": str(pat_a.id),
        "doctor_id": str(doc_a.id),
        "scheduled_at": now_utc.isoformat(),
        "duration_minutes": 30,
        "notes": "First consultation"
    }

    create_res = client.post("/api/v1/appointments", json=appt_payload, headers=headers)
    assert create_res.status_code == 201
    data = create_res.json()
    assert data["patient"]["full_name"] == "Patient Alpha"
    assert data["doctor"]["full_name"] == "Dr. Alpha"
    assert data["status"] == "SCHEDULED"
    appt_id = data["id"]

    # List appointments
    list_res = client.get(f"/api/v1/appointments?start_date={now_utc.date().isoformat()}", headers=headers)
    assert list_res.status_code == 200
    items = list_res.json()["items"]
    assert len(items) == 1
    assert items[0]["id"] == appt_id


def test_doctor_conflict_detection(client: TestClient, appt_multi_tenant_fixture):
    """Verify 409 Conflict when attempting to book an overlapping appointment with same doctor."""
    (c1, user_a, doc_a, pat_a), _ = appt_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    base_time = datetime.now(timezone.utc).replace(microsecond=0) + timedelta(days=2)

    # 1. Book initial appointment 10:00 - 10:30
    appt1_payload = {
        "patient_id": str(pat_a.id),
        "doctor_id": str(doc_a.id),
        "scheduled_at": base_time.isoformat(),
        "duration_minutes": 30
    }
    assert client.post("/api/v1/appointments", json=appt1_payload, headers=headers).status_code == 201

    # 2. Attempt overlapping appointment 10:15 - 10:45 (Conflict!)
    overlap_time = base_time + timedelta(minutes=15)
    conflict_payload = {
        "patient_id": str(pat_a.id),
        "doctor_id": str(doc_a.id),
        "scheduled_at": overlap_time.isoformat(),
        "duration_minutes": 30
    }
    conflict_res = client.post("/api/v1/appointments", json=conflict_payload, headers=headers)
    assert conflict_res.status_code == 409
    assert "already has an active appointment" in conflict_res.json()["detail"]

    # 3. Non-overlapping appointment 10:30 - 11:00 (Success)
    non_overlap_time = base_time + timedelta(minutes=30)
    success_payload = {
        "patient_id": str(pat_a.id),
        "doctor_id": str(doc_a.id),
        "scheduled_at": non_overlap_time.isoformat(),
        "duration_minutes": 30
    }
    assert client.post("/api/v1/appointments", json=success_payload, headers=headers).status_code == 201


def test_appointment_status_transitions(client: TestClient, appt_multi_tenant_fixture):
    """Verify status updates SCHEDULED -> CONFIRMED -> COMPLETED."""
    (c1, user_a, doc_a, pat_a), _ = appt_multi_tenant_fixture
    token = create_access_token(subject=str(user_a.id), clinic_id=str(c1.id), role=user_a.role.value)
    headers = {"Authorization": f"Bearer {token}"}

    scheduled_at = datetime.now(timezone.utc).replace(microsecond=0) + timedelta(days=3)
    create_res = client.post(
        "/api/v1/appointments",
        json={
            "patient_id": str(pat_a.id),
            "doctor_id": str(doc_a.id),
            "scheduled_at": scheduled_at.isoformat(),
            "duration_minutes": 30
        },
        headers=headers
    )
    appt_id = create_res.json()["id"]

    # Confirm
    conf_res = client.patch(f"/api/v1/appointments/{appt_id}/status", json={"status": "CONFIRMED"}, headers=headers)
    assert conf_res.status_code == 200
    assert conf_res.json()["status"] == "CONFIRMED"

    # Complete
    comp_res = client.patch(f"/api/v1/appointments/{appt_id}/status", json={"status": "COMPLETED"}, headers=headers)
    assert comp_res.status_code == 200
    assert comp_res.json()["status"] == "COMPLETED"


def test_cross_tenant_appointment_isolation(client: TestClient, appt_multi_tenant_fixture, db_session: Session):
    """Verify Clinic A appointment cannot be accessed or updated by Clinic B."""
    (c1, user_a, doc_a, pat_a), (c2, user_b, _, _) = appt_multi_tenant_fixture

    appt = Appointment(
        clinic_id=c1.id,
        patient_id=pat_a.id,
        doctor_id=doc_a.id,
        scheduled_at=datetime.now(timezone.utc) + timedelta(days=4),
        duration_minutes=30,
        status=AppointmentStatus.SCHEDULED
    )
    db_session.add(appt)
    db_session.commit()
    db_session.refresh(appt)

    token_b = create_access_token(subject=str(user_b.id), clinic_id=str(c2.id), role=user_b.role.value)
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # Clinic B GET returns 404
    assert client.get(f"/api/v1/appointments/{appt.id}", headers=headers_b).status_code == 404

    # Clinic B status update returns 404
    assert client.patch(f"/api/v1/appointments/{appt.id}/status", json={"status": "CANCELLED"}, headers=headers_b).status_code == 404
