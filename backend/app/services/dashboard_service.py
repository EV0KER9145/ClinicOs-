import zoneinfo
from datetime import datetime, time, timezone
from sqlalchemy import func, desc, asc
from sqlalchemy.orm import Session, joinedload
from app.models.appointment import Appointment, AppointmentStatus
from app.models.clinic import Clinic
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead
from app.models.patient import Patient
from app.models.user import User
from app.schemas.dashboard import (
    AttentionSummary,
    DashboardSummaryResponse,
    RecentNoShowSummary,
    TodayMetrics,
    UpcomingAppointmentSummary,
)


class DashboardService:

    @staticmethod
    def get_summary(db: Session, current_user: User) -> DashboardSummaryResponse:
        clinic = db.query(Clinic).filter(Clinic.id == current_user.clinic_id).first()
        clinic_name = clinic.name if clinic else "ClinicOS"
        tz_name = clinic.timezone if clinic and clinic.timezone else "Asia/Kolkata"

        # Resolve current date in clinic local timezone
        try:
            local_tz = zoneinfo.ZoneInfo(tz_name)
        except Exception:
            local_tz = zoneinfo.ZoneInfo("Asia/Kolkata")

        now_utc = datetime.now(timezone.utc)
        now_local = now_utc.astimezone(local_tz)
        today_local_date = now_local.date()

        # Local day boundaries in UTC
        start_local = datetime.combine(today_local_date, time.min).replace(tzinfo=local_tz)
        end_local = datetime.combine(today_local_date, time.max).replace(tzinfo=local_tz)

        start_utc = start_local.astimezone(timezone.utc)
        end_utc = end_local.astimezone(timezone.utc)

        clinic_id = current_user.clinic_id

        # 1. Appointments Today Metrics
        appts_count = db.query(func.count(Appointment.id)).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.scheduled_at >= start_utc,
            Appointment.scheduled_at <= end_utc,
            Appointment.status.in_([AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED])
        ).scalar() or 0

        completed_appts_count = db.query(func.count(Appointment.id)).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.scheduled_at >= start_utc,
            Appointment.scheduled_at <= end_utc,
            Appointment.status == AppointmentStatus.COMPLETED
        ).scalar() or 0

        no_shows_today_count = db.query(func.count(Appointment.id)).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.scheduled_at >= start_utc,
            Appointment.scheduled_at <= end_utc,
            Appointment.status == AppointmentStatus.NO_SHOW
        ).scalar() or 0

        # 2. Leads & Patients Created Today
        new_leads_count = db.query(func.count(Lead.id)).filter(
            Lead.clinic_id == clinic_id,
            Lead.created_at >= start_utc,
            Lead.created_at <= end_utc,
            Lead.is_active.is_(True)
        ).scalar() or 0

        new_patients_count = db.query(func.count(Patient.id)).filter(
            Patient.clinic_id == clinic_id,
            Patient.created_at >= start_utc,
            Patient.created_at <= end_utc,
            Patient.is_active.is_(True)
        ).scalar() or 0

        # 3. Follow-up Task Metrics
        pending_follow_ups_count = db.query(func.count(FollowUp.id)).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.is_active.is_(True)
        ).scalar() or 0

        overdue_follow_ups_count = db.query(func.count(FollowUp.id)).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at < now_utc,
            FollowUp.is_active.is_(True)
        ).scalar() or 0

        due_today_follow_ups_count = db.query(func.count(FollowUp.id)).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at >= start_utc,
            FollowUp.due_at <= end_utc,
            FollowUp.is_active.is_(True)
        ).scalar() or 0

        # 4. Upcoming Appointments (Next 5)
        upcoming_raw = db.query(Appointment).options(
            joinedload(Appointment.patient),
            joinedload(Appointment.doctor)
        ).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.status.in_([AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED]),
            Appointment.scheduled_at >= now_utc
        ).order_by(asc(Appointment.scheduled_at)).limit(5).all()

        upcoming_list = [
            UpcomingAppointmentSummary(
                id=appt.id,
                scheduled_at=appt.scheduled_at,
                duration_minutes=appt.duration_minutes,
                patient_name=appt.patient.full_name if appt.patient else "Patient",
                doctor_name=appt.doctor.full_name if appt.doctor else "Doctor",
                status=appt.status
            )
            for appt in upcoming_raw
        ]

        # 5. Recent No-Shows (Last 5)
        no_shows_raw = db.query(Appointment).options(
            joinedload(Appointment.patient),
            joinedload(Appointment.doctor)
        ).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.status == AppointmentStatus.NO_SHOW
        ).order_by(desc(Appointment.scheduled_at)).limit(5).all()

        no_shows_list = [
            RecentNoShowSummary(
                id=appt.id,
                scheduled_at=appt.scheduled_at,
                patient_name=appt.patient.full_name if appt.patient else "Patient",
                doctor_name=appt.doctor.full_name if appt.doctor else "Doctor"
            )
            for appt in no_shows_raw
        ]

        today_metrics = TodayMetrics(
            date=today_local_date.isoformat(),
            appointments_count=appts_count,
            completed_appointments=completed_appts_count,
            no_shows_count=no_shows_today_count,
            new_leads_count=new_leads_count,
            new_patients_count=new_patients_count,
            pending_follow_ups_count=pending_follow_ups_count,
            overdue_follow_ups_count=overdue_follow_ups_count,
            due_today_follow_ups_count=due_today_follow_ups_count
        )

        attention = AttentionSummary(
            overdue_follow_ups=overdue_follow_ups_count,
            no_shows_today=no_shows_today_count,
            new_leads=new_leads_count
        )

        return DashboardSummaryResponse(
            clinic_name=clinic_name,
            today_date=today_local_date.strftime("%A, %d %B %Y"),
            user_name=current_user.full_name,
            user_role=current_user.role.value,
            today=today_metrics,
            attention=attention,
            upcoming_appointments=upcoming_list,
            recent_no_shows=no_shows_list
        )
