import logging
import uuid
from datetime import datetime, timedelta, timezone
from typing import List, Optional
from sqlalchemy import func, or_
from sqlalchemy.orm import Session
from app.automation.events import AutomationEvent, AutomationEventType
from app.models.appointment import Appointment, AppointmentStatus
from app.models.automation import AutomationExecution, CommunicationDraft, DraftStatus, ExecutionStatus
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead, LeadStatus
from app.models.notification import NotificationType
from app.models.patient import Patient
from app.models.user import User, UserRole
from app.services.notification_service import NotificationService

logger = logging.getLogger("clinicos.automation")


class AutomationService:

    @staticmethod
    def is_already_executed(db: Session, idempotency_key: str) -> bool:
        """Check if an automation rule has already been executed for the given key."""
        existing = db.query(AutomationExecution).filter(
            AutomationExecution.idempotency_key == idempotency_key
        ).first()
        return existing is not None

    @staticmethod
    def _record_execution(
        db: Session,
        clinic_id: uuid.UUID,
        event_type: str,
        rule_type: str,
        entity_type: str,
        entity_id: uuid.UUID,
        action_type: str,
        idempotency_key: str,
        status: ExecutionStatus = ExecutionStatus.SUCCESS,
        error_message: Optional[str] = None
    ) -> AutomationExecution:
        execution = AutomationExecution(
            clinic_id=clinic_id,
            event_type=event_type,
            rule_type=rule_type,
            entity_type=entity_type,
            entity_id=entity_id,
            action_type=action_type,
            status=status,
            idempotency_key=idempotency_key,
            executed_at=datetime.now(timezone.utc),
            error_message=error_message
        )
        db.add(execution)
        db.flush()
        return execution

    # ==========================================
    # RULE 1 — NO-SHOW RECOVERY
    # ==========================================
    @staticmethod
    def process_no_show_recovery(db: Session, event: AutomationEvent):
        idempotency_key = f"{event.clinic_id}:NO_SHOW_RECOVERY:{event.entity_id}"
        if AutomationService.is_already_executed(db, idempotency_key):
            logger.info(f"Skipping no-show recovery rule: already executed for key {idempotency_key}")
            return

        appt = db.query(Appointment).filter(
            Appointment.id == event.entity_id,
            Appointment.clinic_id == event.clinic_id
        ).first()

        if not appt or appt.status != AppointmentStatus.NO_SHOW:
            return

        # Check if patient already has a future scheduled appointment
        now_utc = datetime.now(timezone.utc)
        future_appt = db.query(Appointment).filter(
            Appointment.patient_id == appt.patient_id,
            Appointment.clinic_id == appt.clinic_id,
            Appointment.status.in_([AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED]),
            Appointment.scheduled_at >= now_utc
        ).first()

        if future_appt:
            # Patient already rescheduled; skip creating recovery follow-up
            AutomationService._record_execution(
                db, event.clinic_id, event.event_type.value, "NO_SHOW_RECOVERY",
                "APPOINTMENT", appt.id, "SKIPPED_FUTURE_APPT_EXISTS", idempotency_key, status=ExecutionStatus.SKIPPED
            )
            return

        # Find primary owner or staff assignee for the clinic
        assignee = db.query(User).filter(
            User.clinic_id == appt.clinic_id,
            User.is_active.is_(True)
        ).order_by(User.created_at.asc()).first()

        assignee_id = assignee.id if assignee else appt.clinic_id
        patient_name = appt.patient.full_name if appt.patient else "Patient"

        # 1. Create Recovery Follow-up Task
        follow_up = FollowUp(
            clinic_id=appt.clinic_id,
            assigned_user_id=assignee_id,
            patient_id=appt.patient_id,
            title=f"Follow-up: Reschedule Missed Appointment for {patient_name}",
            notes="Automated: Created following no-show appointment recovery rule.",
            due_at=now_utc + timedelta(hours=24),
            status=FollowUpStatus.PENDING,
            is_active=True
        )
        db.add(follow_up)
        db.flush()

        # 2. Create In-App Notification
        if assignee:
            NotificationService.create_notification(
                db=db,
                clinic_id=appt.clinic_id,
                user_id=assignee_id,
                title="Missed Appointment Recovery Task",
                message=f"{patient_name} missed their appointment. A recovery follow-up task has been scheduled.",
                type=NotificationType.APPOINTMENT,
                entity_type="FOLLOW_UP",
                entity_id=follow_up.id
            )

        # 3. Create Communication Draft for Patient Outreach
        draft_msg = f"Hi {patient_name.split()[0]}, we missed you at your recent appointment at {appt.clinic.name if appt.clinic else 'the clinic'}. Would you like us to help you reschedule a convenient time?"
        draft = CommunicationDraft(
            clinic_id=appt.clinic_id,
            entity_type="APPOINTMENT",
            entity_id=appt.id,
            recipient_name=patient_name,
            recipient_phone=appt.patient.phone if appt.patient else None,
            purpose="NO_SHOW_RECOVERY",
            message_content=draft_msg,
            status=DraftStatus.DRAFT,
            created_by_user_id=assignee_id
        )
        db.add(draft)

        # Record Execution
        AutomationService._record_execution(
            db, appt.clinic_id, event.event_type.value, "NO_SHOW_RECOVERY",
            "APPOINTMENT", appt.id, "CREATE_RECOVERY_TASK_AND_DRAFT", idempotency_key
        )

    # ==========================================
    # TIME-DRIVEN AUTOMATION EVALUATOR
    # ==========================================
    @staticmethod
    def evaluate_time_driven_rules(db: Session, clinic_id: uuid.UUID) -> int:
        """Evaluate scheduled time-driven automation rules for the given clinic."""
        now_utc = datetime.now(timezone.utc)
        today_date_str = now_utc.strftime("%Y-%m-%d")
        executed_count = 0

        # 1. Follow-up Due Today Reminder Rule
        start_today = datetime.combine(now_utc.date(), datetime.min.time()).replace(tzinfo=timezone.utc)
        end_today = datetime.combine(now_utc.date(), datetime.max.time()).replace(tzinfo=timezone.utc)

        due_today_fus = db.query(FollowUp).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at >= start_today,
            FollowUp.due_at <= end_today,
            FollowUp.is_active.is_(True)
        ).all()

        for fu in due_today_fus:
            key = f"{clinic_id}:FOLLOW_UP_DUE_TODAY:{fu.id}:{today_date_str}"
            if not AutomationService.is_already_executed(db, key):
                NotificationService.create_notification(
                    db=db,
                    clinic_id=clinic_id,
                    user_id=fu.assigned_user_id,
                    title="Follow-up Due Today",
                    message=f"Reminder: Follow-up task '{fu.title}' is due today.",
                    type=NotificationType.FOLLOW_UP,
                    entity_type="FOLLOW_UP",
                    entity_id=fu.id
                )
                AutomationService._record_execution(
                    db, clinic_id, AutomationEventType.FOLLOW_UP_DUE.value,
                    "FOLLOW_UP_DUE_TODAY", "FOLLOW_UP", fu.id, "CREATE_NOTIFICATION", key
                )
                executed_count += 1

        # 2. Overdue Follow-up Alert Rule
        overdue_fus = db.query(FollowUp).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at < now_utc,
            FollowUp.is_active.is_(True)
        ).all()

        for fu in overdue_fus:
            key = f"{clinic_id}:FOLLOW_UP_OVERDUE_ALERT:{fu.id}:{today_date_str}"
            if not AutomationService.is_already_executed(db, key):
                NotificationService.create_notification(
                    db=db,
                    clinic_id=clinic_id,
                    user_id=fu.assigned_user_id,
                    title="Overdue Follow-up Task Alert ⚠",
                    message=f"Task '{fu.title}' is overdue. Please complete or reschedule.",
                    type=NotificationType.FOLLOW_UP,
                    entity_type="FOLLOW_UP",
                    entity_id=fu.id
                )
                AutomationService._record_execution(
                    db, clinic_id, AutomationEventType.FOLLOW_UP_OVERDUE.value,
                    "FOLLOW_UP_OVERDUE_ALERT", "FOLLOW_UP", fu.id, "CREATE_NOTIFICATION", key
                )
                executed_count += 1

        # 3. Uncontacted / Inactive Lead Automation
        inactive_threshold = now_utc - timedelta(days=7)
        inactive_leads = db.query(Lead).filter(
            Lead.clinic_id == clinic_id,
            Lead.status.in_([LeadStatus.NEW, LeadStatus.CONTACTED]),
            Lead.updated_at < inactive_threshold,
            Lead.is_active.is_(True)
        ).all()

        for lead in inactive_leads:
            week_str = now_utc.strftime("%Y-W%U")
            key = f"{clinic_id}:INACTIVE_LEAD_REENGAGEMENT:{lead.id}:{week_str}"
            if not AutomationService.is_already_executed(db, key):
                assignee_id = lead.assigned_user_id
                if not assignee_id:
                    owner = db.query(User).filter(User.clinic_id == clinic_id, User.is_active.is_(True)).first()
                    assignee_id = owner.id if owner else clinic_id

                # Create Re-engagement Follow-up
                fu_lead = FollowUp(
                    clinic_id=clinic_id,
                    assigned_user_id=assignee_id,
                    lead_id=lead.id,
                    title=f"Re-engage Inactive Lead: {lead.full_name}",
                    notes="Automated: Lead has had no activity for over 7 days.",
                    due_at=now_utc + timedelta(hours=24),
                    status=FollowUpStatus.PENDING,
                    is_active=True
                )
                db.add(fu_lead)
                db.flush()

                # Create Communication Draft
                draft = CommunicationDraft(
                    clinic_id=clinic_id,
                    entity_type="LEAD",
                    entity_id=lead.id,
                    recipient_name=lead.full_name,
                    recipient_phone=lead.phone,
                    purpose="LEAD_REENGAGEMENT",
                    message_content=f"Hi {lead.full_name.split()[0]}, we are checking in regarding your enquiry with us. Please let us know if we can answer any questions or help book a consultation.",
                    status=DraftStatus.DRAFT,
                    created_by_user_id=assignee_id
                )
                db.add(draft)

                AutomationService._record_execution(
                    db, clinic_id, AutomationEventType.LEAD_INACTIVE.value,
                    "INACTIVE_LEAD_REENGAGEMENT", "LEAD", lead.id, "CREATE_REENGAGEMENT_TASK_AND_DRAFT", key
                )
                executed_count += 1

        db.commit()
        return executed_count

    # ==========================================
    # CENTRAL DISPATCHER
    # ==========================================
    @staticmethod
    def dispatch_event(db: Session, event: AutomationEvent):
        """Dispatch domain event to applicable automation rules safely."""
        try:
            if event.event_type == AutomationEventType.APPOINTMENT_NO_SHOW:
                AutomationService.process_no_show_recovery(db, event)
            db.commit()
        except Exception as e:
            db.rollback()
            logger.error(f"Error executing automation rule for event {event.event_type.value}", exc_info=e)
