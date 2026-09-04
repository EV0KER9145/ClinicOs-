import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum as PyEnum
from typing import Any, Dict, Optional


class AutomationEventType(str, PyEnum):
    APPOINTMENT_NO_SHOW = "APPOINTMENT_NO_SHOW"
    APPOINTMENT_UPCOMING = "APPOINTMENT_UPCOMING"
    FOLLOW_UP_DUE = "FOLLOW_UP_DUE"
    FOLLOW_UP_OVERDUE = "FOLLOW_UP_OVERDUE"
    LEAD_CREATED = "LEAD_CREATED"
    LEAD_UNCONTACTED = "LEAD_UNCONTACTED"
    LEAD_INACTIVE = "LEAD_INACTIVE"
    PATIENT_INACTIVE = "PATIENT_INACTIVE"


@dataclass
class AutomationEvent:
    event_type: AutomationEventType
    clinic_id: uuid.UUID
    entity_type: str  # APPOINTMENT, FOLLOW_UP, LEAD, PATIENT
    entity_id: uuid.UUID
    occurred_at: datetime = field(default_factory=lambda: datetime.now(timezone.utc))
    metadata: Dict[str, Any] = field(default_factory=dict)
