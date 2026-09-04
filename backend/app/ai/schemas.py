import uuid
from enum import Enum as PyEnum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, field_validator


class RecommendationPriority(str, PyEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    URGENT = "URGENT"


class RecommendationCategory(str, PyEnum):
    OVERDUE_FOLLOW_UP = "OVERDUE_FOLLOW_UP"
    APPOINTMENT_NO_SHOW = "APPOINTMENT_NO_SHOW"
    INACTIVE_LEAD = "INACTIVE_LEAD"
    UNCONTACTED_LEAD = "UNCONTACTED_LEAD"


class FollowUpRecommendation(BaseModel):
    entity_type: str  # FOLLOW_UP, APPOINTMENT, LEAD, PATIENT
    entity_id: uuid.UUID
    category: RecommendationCategory
    priority: RecommendationPriority
    entity_name: str
    reason: str
    suggested_action: str
    suggested_message: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class FollowUpRecommendationsResponse(BaseModel):
    recommendations: List[FollowUpRecommendation] = []
    total_candidates: int = 0


class MessagePurpose(str, PyEnum):
    FOLLOW_UP = "FOLLOW_UP"
    NO_SHOW_RECOVERY = "NO_SHOW_RECOVERY"
    LEAD_REENGAGEMENT = "LEAD_REENGAGEMENT"
    APPOINTMENT_REMINDER = "APPOINTMENT_REMINDER"
    PATIENT_REACTIVATION = "PATIENT_REACTIVATION"
    GENERAL_ADMINISTRATIVE = "GENERAL_ADMINISTRATIVE"


class MessageTone(str, PyEnum):
    PROFESSIONAL = "PROFESSIONAL"
    FRIENDLY = "FRIENDLY"
    BRIEF = "BRIEF"


class GenerateMessageRequest(BaseModel):
    entity_type: str  # PATIENT, LEAD, APPOINTMENT, FOLLOW_UP
    entity_id: uuid.UUID
    purpose: MessagePurpose
    tone: MessageTone = MessageTone.FRIENDLY
    additional_context: Optional[str] = None


class GenerateMessageResponse(BaseModel):
    entity_type: str
    entity_id: uuid.UUID
    recipient_name: str
    message: str
    purpose: MessagePurpose
    tone: MessageTone


class InsightCategory(str, PyEnum):
    FOLLOW_UP_ATTENTION = "FOLLOW_UP_ATTENTION"
    NO_SHOW_PATTERN = "NO_SHOW_PATTERN"
    LEAD_CONVERSION = "LEAD_CONVERSION"
    PATIENT_RETENTION = "PATIENT_RETENTION"
    APPOINTMENT_TREND = "APPOINTMENT_TREND"
    OPERATIONAL_ALERT = "OPERATIONAL_ALERT"


class ClinicInsight(BaseModel):
    title: str
    category: InsightCategory
    priority: RecommendationPriority
    summary: str
    why_it_matters: str
    suggested_action: str
    action_route: Optional[str] = None
    supporting_metrics: Dict[str, Any] = {}


class ClinicInsightsResponse(BaseModel):
    clinic_name: str
    period: str
    insights: List[ClinicInsight] = []


class AnalyticsIntent(str, PyEnum):
    APPOINTMENT_COUNT = "APPOINTMENT_COUNT"
    NO_SHOW_COUNT = "NO_SHOW_COUNT"
    LEAD_SOURCE_ANALYSIS = "LEAD_SOURCE_ANALYSIS"
    LEAD_CONVERSION_RATE = "LEAD_CONVERSION_RATE"
    OVERDUE_FOLLOW_UP_COUNT = "OVERDUE_FOLLOW_UP_COUNT"
    NEW_PATIENT_COUNT = "NEW_PATIENT_COUNT"
    UNSUPPORTED = "UNSUPPORTED"
    CLINICAL_PROHIBITED = "CLINICAL_PROHIBITED"


class AnalyticsPeriod(str, PyEnum):
    TODAY = "TODAY"
    THIS_WEEK = "THIS_WEEK"
    LAST_WEEK = "LAST_WEEK"
    THIS_MONTH = "THIS_MONTH"
    LAST_MONTH = "LAST_MONTH"


class AnalyticsQueryRequest(BaseModel):
    question: str
    period: Optional[AnalyticsPeriod] = None

    @field_validator("question")
    @classmethod
    def validate_question(cls, v: str) -> str:
        if not v.strip():
            raise ValueError("Question cannot be blank.")
        return v.strip()


class AnalyticsQueryResponse(BaseModel):
    question: str
    intent: AnalyticsIntent
    period: str
    answer: str
    structured_metrics: Dict[str, Any] = {}
    suggested_actions: List[str] = []
