import uuid
from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, ConfigDict
from app.models.appointment import AppointmentStatus


class TodayMetrics(BaseModel):
    date: str
    appointments_count: int
    completed_appointments: int
    no_shows_count: int
    new_leads_count: int
    new_patients_count: int
    pending_follow_ups_count: int
    overdue_follow_ups_count: int
    due_today_follow_ups_count: int


class AttentionSummary(BaseModel):
    overdue_follow_ups: int
    no_shows_today: int
    new_leads: int


class UpcomingAppointmentSummary(BaseModel):
    id: uuid.UUID
    scheduled_at: datetime
    duration_minutes: int
    patient_name: str
    doctor_name: str
    status: AppointmentStatus

    model_config = ConfigDict(from_attributes=True)


class RecentNoShowSummary(BaseModel):
    id: uuid.UUID
    scheduled_at: datetime
    patient_name: str
    doctor_name: str

    model_config = ConfigDict(from_attributes=True)


class DashboardSummaryResponse(BaseModel):
    clinic_name: str
    today_date: str
    user_name: str
    user_role: str
    today: TodayMetrics
    attention: AttentionSummary
    upcoming_appointments: List[UpcomingAppointmentSummary] = []
    recent_no_shows: List[RecentNoShowSummary] = []
