import uuid
from datetime import datetime, time, timedelta, timezone
from typing import Any, Dict, List, Optional
from sqlalchemy import func, desc, or_
from sqlalchemy.orm import Session
from app.ai.providers.gemini import GeminiAIProvider
from app.ai.safety import MedicalSafetyChecker
from app.ai.schemas import (
    AnalyticsIntent,
    AnalyticsPeriod,
    AnalyticsQueryResponse,
    ClinicInsight,
    ClinicInsightsResponse,
    FollowUpRecommendation,
    FollowUpRecommendationsResponse,
    GenerateMessageResponse,
    InsightCategory,
    MessagePurpose,
    MessageTone,
    RecommendationCategory,
    RecommendationPriority,
)
from app.models.appointment import Appointment, AppointmentStatus
from app.models.clinic import Clinic
from app.models.follow_up import FollowUp, FollowUpStatus
from app.models.lead import Lead, LeadStatus
from app.models.patient import Patient
from app.models.user import User
from app.services.dashboard_service import DashboardService


class AIService:

    def __init__(self):
        self.provider = GeminiAIProvider()

    # ==========================================
    # C1 — AI FOLLOW-UP INTELLIGENCE
    # ==========================================
    def get_follow_up_recommendations(
        self,
        db: Session,
        current_user: User,
        limit: int = 10
    ) -> FollowUpRecommendationsResponse:
        """Deterministically detect candidates and generate AI follow-up prioritization recommendations."""
        clinic_id = current_user.clinic_id
        now_utc = datetime.now(timezone.utc)
        recommendations: List[FollowUpRecommendation] = []

        # 1. Candidate 1: Overdue Pending Follow-ups
        overdue_fus = db.query(FollowUp).filter(
            FollowUp.clinic_id == clinic_id,
            FollowUp.status == FollowUpStatus.PENDING,
            FollowUp.due_at < now_utc,
            FollowUp.is_active.is_(True)
        ).order_by(FollowUp.due_at.asc()).limit(5).all()

        for fu in overdue_fus:
            name = fu.patient.full_name if fu.patient else (fu.lead.full_name if fu.lead else "Task")
            phone = fu.patient.phone if fu.patient else (fu.lead.phone if fu.lead else "")
            recommendations.append(
                FollowUpRecommendation(
                    entity_type="FOLLOW_UP",
                    entity_id=fu.id,
                    category=RecommendationCategory.OVERDUE_FOLLOW_UP,
                    priority=RecommendationPriority.URGENT,
                    entity_name=name,
                    reason=f"Task '{fu.title}' was due on {fu.due_at.strftime('%d %b %H:%M')}.",
                    suggested_action="Call or message to complete follow-up.",
                    suggested_message=f"Hi {name.split()[0]}, following up regarding '{fu.title}'. Please let us know if we can assist you."
                )
            )

        # 2. Candidate 2: Recent No-Shows
        no_shows = db.query(Appointment).filter(
            Appointment.clinic_id == clinic_id,
            Appointment.status == AppointmentStatus.NO_SHOW
        ).order_by(desc(Appointment.scheduled_at)).limit(3).all()

        for appt in no_shows:
            patient_name = appt.patient.full_name if appt.patient else "Patient"
            recommendations.append(
                FollowUpRecommendation(
                    entity_type="APPOINTMENT",
                    entity_id=appt.id,
                    category=RecommendationCategory.APPOINTMENT_NO_SHOW,
                    priority=RecommendationPriority.HIGH,
                    entity_name=patient_name,
                    reason=f"Missed appointment on {appt.scheduled_at.strftime('%d %b')} with {appt.doctor.full_name if appt.doctor else 'Doctor'}.",
                    suggested_action="Contact patient to reschedule appointment.",
                    suggested_message=f"Hi {patient_name.split()[0]}, we missed you at your recent appointment. Would you like us to help you reschedule a convenient time?"
                )
            )

        # 3. Candidate 3: Uncontacted / New Leads
        new_leads = db.query(Lead).filter(
            Lead.clinic_id == clinic_id,
            Lead.status == LeadStatus.NEW,
            Lead.is_active.is_(True)
        ).order_by(desc(Lead.created_at)).limit(3).all()

        for lead in new_leads:
            recommendations.append(
                FollowUpRecommendation(
                    entity_type="LEAD",
                    entity_id=lead.id,
                    category=RecommendationCategory.UNCONTACTED_LEAD,
                    priority=RecommendationPriority.MEDIUM,
                    entity_name=lead.full_name,
                    reason=f"New enquiry captured via {lead.source.value} regarding '{lead.interested_service or 'Clinic Services'}'.",
                    suggested_action="Initiate first contact and answer initial enquiry questions.",
                    suggested_message=f"Hi {lead.full_name.split()[0]}, thank you for reaching out to us via {lead.source.value}. How can we assist you today?"
                )
            )

        return FollowUpRecommendationsResponse(
            recommendations=recommendations[:limit],
            total_candidates=len(recommendations)
        )

    # ==========================================
    # C2 — AI MESSAGE ASSISTANT
    # ==========================================
    def generate_message(
        self,
        db: Session,
        current_user: User,
        entity_type: str,
        entity_id: uuid.UUID,
        purpose: MessagePurpose,
        tone: MessageTone = MessageTone.FRIENDLY,
        additional_context: Optional[str] = None
    ) -> GenerateMessageResponse:
        """Generate tailored administrative outreach message."""
        clinic = db.query(Clinic).filter(Clinic.id == current_user.clinic_id).first()
        clinic_name = clinic.name if clinic else "ClinicOS"

        recipient_name = "Patient"
        phone = ""
        context_info = ""

        entity_type_upper = entity_type.upper()

        if entity_type_upper == "PATIENT":
            patient = db.query(Patient).filter(Patient.id == entity_id, Patient.clinic_id == current_user.clinic_id).first()
            if patient:
                recipient_name = patient.full_name
                phone = patient.phone or ""
        elif entity_type_upper == "LEAD":
            lead = db.query(Lead).filter(Lead.id == entity_id, Lead.clinic_id == current_user.clinic_id).first()
            if lead:
                recipient_name = lead.full_name
                phone = lead.phone or ""
                context_info = f"Enquiry regarding {lead.interested_service or 'services'} via {lead.source.value}."
        elif entity_type_upper == "APPOINTMENT":
            appt = db.query(Appointment).filter(Appointment.id == entity_id, Appointment.clinic_id == current_user.clinic_id).first()
            if appt and appt.patient:
                recipient_name = appt.patient.full_name
                context_info = f"Appointment with {appt.doctor.full_name if appt.doctor else 'Doctor'} at {appt.scheduled_at.strftime('%d %b %I:%M %p')}."
        elif entity_type_upper == "FOLLOW_UP":
            fu = db.query(FollowUp).filter(FollowUp.id == entity_id, FollowUp.clinic_id == current_user.clinic_id).first()
            if fu:
                recipient_name = fu.patient.full_name if fu.patient else (fu.lead.full_name if fu.lead else "Patient")
                context_info = f"Follow-up task: '{fu.title}'."

        first_name = recipient_name.split()[0]
        sanitized_extra = MedicalSafetyChecker.sanitize_untrusted_text(additional_context or "")

        # Try generating via LLM if available
        prompt = (
            f"Draft a {tone.value.lower()} administrative message for a clinic named '{clinic_name}'.\n"
            f"Recipient Name: {recipient_name}\n"
            f"Purpose: {purpose.value}\n"
            f"Context: {context_info}\n"
            f"Extra note: {sanitized_extra}\n"
            f"REQUIREMENTS: Keep it respectful, non-clinical, brief (1-3 sentences), and do not include diagnosis or medical advice."
        )

        ai_msg = self.provider.generate_text(prompt, system_instruction="You are a helpful clinic receptionist assistant.")

        if not ai_msg:
            # Deterministic Fallback Template Generator
            if purpose == MessagePurpose.NO_SHOW_RECOVERY:
                ai_msg = f"Hi {first_name}, we missed you at your recent appointment at {clinic_name}. Would you like us to help you reschedule for another time?"
            elif purpose == MessagePurpose.LEAD_REENGAGEMENT:
                ai_msg = f"Hi {first_name}, thank you for inquiring with {clinic_name}. Let us know if you have any questions or if you'd like to book a consultation."
            elif purpose == MessagePurpose.APPOINTMENT_REMINDER:
                ai_msg = f"Hi {first_name}, this is a gentle reminder for your upcoming appointment at {clinic_name}. Please let us know if you need to adjust your schedule."
            else:
                ai_msg = f"Hi {first_name}, this is {clinic_name} following up regarding your recent enquiry. Please feel free to reply if you need any assistance."

        return GenerateMessageResponse(
            entity_type=entity_type_upper,
            entity_id=entity_id,
            recipient_name=recipient_name,
            message=ai_msg.strip(),
            purpose=purpose,
            tone=tone
        )

    # ==========================================
    # C3 — AI CLINIC INSIGHTS
    # ==========================================
    def get_clinic_insights(
        self,
        db: Session,
        current_user: User
    ) -> ClinicInsightsResponse:
        """Generate operational insights based on deterministic clinic metrics."""
        summary = DashboardService.get_summary(db, current_user)
        insights: List[ClinicInsight] = []

        # 1. Check Follow-up Backlog
        if summary.today.overdue_follow_ups_count > 0:
            insights.append(
                ClinicInsight(
                    title="Overdue Follow-up Task Backlog",
                    category=InsightCategory.FOLLOW_UP_ATTENTION,
                    priority=RecommendationPriority.HIGH,
                    summary=f"Your clinic currently has {summary.today.overdue_follow_ups_count} overdue follow-up tasks.",
                    why_it_matters="Delayed callbacks can reduce patient satisfaction and enquiry conversion rates.",
                    suggested_action="Review and assign overdue follow-ups in the Follow-ups Queue.",
                    action_route="follow_ups",
                    supporting_metrics={"overdue_count": summary.today.overdue_follow_ups_count}
                )
            )

        # 2. Check Recent No-Shows
        if summary.today.no_shows_count > 0 or len(summary.recent_no_shows) > 0:
            no_show_num = len(summary.recent_no_shows)
            insights.append(
                ClinicInsight(
                    title="Appointment No-Show Recovery Opportunity",
                    category=InsightCategory.NO_SHOW_PATTERN,
                    priority=RecommendationPriority.MEDIUM,
                    summary=f"{no_show_num} recent appointment no-shows require outreach.",
                    why_it_matters="Reaching out to missed appointments within 24 hours improves reschedule rates.",
                    suggested_action="Contact patients listed under Recent No-Shows on the Dashboard.",
                    action_route="appointments",
                    supporting_metrics={"no_shows_count": no_show_num}
                )
            )

        # 3. Check New Enquiries
        if summary.today.new_leads_count > 0:
            insights.append(
                ClinicInsight(
                    title="New Patient Enquiries Captured",
                    category=InsightCategory.LEAD_CONVERSION,
                    priority=RecommendationPriority.MEDIUM,
                    summary=f"{summary.today.new_leads_count} new patient enquiries were captured today.",
                    why_it_matters="Prompt initial responses lead to higher appointment booking rates.",
                    suggested_action="Assign new leads to staff members and initiate first contact.",
                    action_route="leads",
                    supporting_metrics={"new_leads_today": summary.today.new_leads_count}
                )
            )

        # Fallback default insight for quiet clinics
        if not insights:
            insights.append(
                ClinicInsight(
                    title="Clinic Operations Running Smoothly",
                    category=InsightCategory.OPERATIONAL_ALERT,
                    priority=RecommendationPriority.LOW,
                    summary="All follow-up tasks and appointments are up to date.",
                    why_it_matters="Maintaining prompt callbacks keeps clinic utilization high.",
                    suggested_action="Continue recording new patient enquiries and appointments.",
                    action_route="dashboard",
                    supporting_metrics={"status": "optimal"}
                )
            )

        return ClinicInsightsResponse(
            clinic_name=summary.clinic_name,
            period=summary.today_date,
            insights=insights
        )

    # ==========================================
    # C4 — NATURAL LANGUAGE ANALYTICS
    # ==========================================
    def query_analytics(
        self,
        db: Session,
        current_user: User,
        question: str,
        period: Optional[AnalyticsPeriod] = None
    ) -> AnalyticsQueryResponse:
        """Process natural language questions using deterministic SQL queries."""
        # 1. Enforce Medical Safety Boundary
        is_clinical, clinical_msg = MedicalSafetyChecker.validate_clinical_boundary(question)
        if is_clinical:
            return AnalyticsQueryResponse(
                question=question,
                intent=AnalyticsIntent.CLINICAL_PROHIBITED,
                period="N/A",
                answer=clinical_msg,
                structured_metrics={},
                suggested_actions=["Ask about appointments, follow-ups, leads, or patient counts."]
            )

        q_lower = question.lower()
        clinic_id = current_user.clinic_id

        # Determine Intent
        intent = AnalyticsIntent.UNSUPPORTED
        if "no-show" in q_lower or "missed" in q_lower or "no show" in q_lower:
            intent = AnalyticsIntent.NO_SHOW_COUNT
        elif "appointment" in q_lower or "appt" in q_lower or "scheduled" in q_lower:
            intent = AnalyticsIntent.APPOINTMENT_COUNT
        elif "lead" in q_lower or "enquiry" in q_lower or "enquiries" in q_lower or "source" in q_lower:
            if "conversion" in q_lower or "converted" in q_lower:
                intent = AnalyticsIntent.LEAD_CONVERSION_RATE
            else:
                intent = AnalyticsIntent.LEAD_SOURCE_ANALYSIS
        elif "follow-up" in q_lower or "overdue" in q_lower or "task" in q_lower:
            intent = AnalyticsIntent.OVERDUE_FOLLOW_UP_COUNT
        elif "patient" in q_lower or "registration" in q_lower:
            intent = AnalyticsIntent.NEW_PATIENT_COUNT

        # Date range calculation
        now_utc = datetime.now(timezone.utc)
        start_dt = now_utc - timedelta(days=30)
        period_str = "Last 30 Days"

        if period == AnalyticsPeriod.TODAY or "today" in q_lower:
            start_dt = datetime.combine(now_utc.date(), time.min).replace(tzinfo=timezone.utc)
            period_str = "Today"
        elif period == AnalyticsPeriod.THIS_WEEK or "week" in q_lower:
            start_dt = now_utc - timedelta(days=7)
            period_str = "Last 7 Days"
        elif period == AnalyticsPeriod.THIS_MONTH or period == AnalyticsPeriod.LAST_MONTH or "month" in q_lower:
            start_dt = now_utc - timedelta(days=30)
            period_str = "Last 30 Days"

        answer = ""
        metrics = {}
        actions = []

        if intent == AnalyticsIntent.NO_SHOW_COUNT:
            cnt = db.query(func.count(Appointment.id)).filter(
                Appointment.clinic_id == clinic_id,
                Appointment.status == AppointmentStatus.NO_SHOW,
                Appointment.scheduled_at >= start_dt
            ).scalar() or 0
            answer = f"Your clinic recorded {cnt} no-show appointment(s) during {period_str.lower()}."
            metrics = {"no_shows": cnt}
            actions = ["View Recent No-Shows on Dashboard", "Schedule recovery follow-ups"]

        elif intent == AnalyticsIntent.APPOINTMENT_COUNT:
            cnt = db.query(func.count(Appointment.id)).filter(
                Appointment.clinic_id == clinic_id,
                Appointment.scheduled_at >= start_dt
            ).scalar() or 0
            answer = f"A total of {cnt} appointment(s) were scheduled during {period_str.lower()}."
            metrics = {"total_appointments": cnt}
            actions = ["View Appointments Calendar"]

        elif intent == AnalyticsIntent.OVERDUE_FOLLOW_UP_COUNT:
            cnt = db.query(func.count(FollowUp.id)).filter(
                FollowUp.clinic_id == clinic_id,
                FollowUp.status == FollowUpStatus.PENDING,
                FollowUp.due_at < now_utc,
                FollowUp.is_active.is_(True)
            ).scalar() or 0
            answer = f"You currently have {cnt} overdue follow-up task(s) requiring attention."
            metrics = {"overdue_follow_ups": cnt}
            actions = ["Open Follow-ups Queue with Overdue filter"]

        elif intent == AnalyticsIntent.NEW_PATIENT_COUNT:
            cnt = db.query(func.count(Patient.id)).filter(
                Patient.clinic_id == clinic_id,
                Patient.created_at >= start_dt,
                Patient.is_active.is_(True)
            ).scalar() or 0
            answer = f"{cnt} new patient(s) were registered during {period_str.lower()}."
            metrics = {"new_patients": cnt}
            actions = ["View Patients List"]

        elif intent == AnalyticsIntent.LEAD_CONVERSION_RATE or intent == AnalyticsIntent.LEAD_SOURCE_ANALYSIS:
            total_leads = db.query(func.count(Lead.id)).filter(
                Lead.clinic_id == clinic_id,
                Lead.created_at >= start_dt
            ).scalar() or 0
            converted_leads = db.query(func.count(Lead.id)).filter(
                Lead.clinic_id == clinic_id,
                Lead.created_at >= start_dt,
                Lead.status == LeadStatus.CONVERTED
            ).scalar() or 0
            rate = round((converted_leads / total_leads * 100), 1) if total_leads > 0 else 0.0
            answer = f"Your clinic captured {total_leads} enquiry lead(s) with {converted_leads} converted to patients ({rate}% conversion rate) during {period_str.lower()}."
            metrics = {"total_leads": total_leads, "converted": converted_leads, "conversion_rate_pct": rate}
            actions = ["View Enquiries & Leads CRM"]

        else:
            answer = "I can help answer questions about appointments, no-shows, follow-up tasks, lead conversion rates, and patient registrations."
            actions = ["How many appointments were scheduled this week?", "How many follow-ups are overdue?", "What is our lead conversion rate?"]

        return AnalyticsQueryResponse(
            question=question,
            intent=intent,
            period=period_str,
            answer=answer,
            structured_metrics=metrics,
            suggested_actions=actions
        )
