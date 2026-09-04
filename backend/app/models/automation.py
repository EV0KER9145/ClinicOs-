import uuid
from datetime import datetime
from enum import Enum as PyEnum
from typing import TYPE_CHECKING, Optional
from sqlalchemy import String, DateTime, Text, ForeignKey, Enum, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base, UUIDMixin, TimestampMixin

if TYPE_CHECKING:
    from app.models.clinic import Clinic
    from app.models.user import User


class ExecutionStatus(str, PyEnum):
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    SKIPPED = "SKIPPED"


class DraftStatus(str, PyEnum):
    DRAFT = "DRAFT"
    APPROVED = "APPROVED"
    DISCARDED = "DISCARDED"
    SENT = "SENT"


class AutomationExecution(Base, UUIDMixin):
    __tablename__ = "automation_executions"
    __table_args__ = (
        UniqueConstraint("idempotency_key", name="uq_automation_idempotency_key"),
    )

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    event_type: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    rule_type: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    entity_type: Mapped[str] = mapped_column(String(50), nullable=False)
    entity_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    action_type: Mapped[str] = mapped_column(String(100), nullable=False)
    status: Mapped[ExecutionStatus] = mapped_column(
        Enum(ExecutionStatus, name="execution_status_enum", native_enum=True),
        nullable=False,
        default=ExecutionStatus.SUCCESS
    )
    idempotency_key: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    executed_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    error_message: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic")


class CommunicationDraft(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "communication_drafts"

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    entity_type: Mapped[str] = mapped_column(String(50), nullable=False)  # PATIENT, LEAD, APPOINTMENT, FOLLOW_UP
    entity_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), nullable=False, index=True)
    recipient_name: Mapped[str] = mapped_column(String(255), nullable=False)
    recipient_phone: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)
    purpose: Mapped[str] = mapped_column(String(100), nullable=False)
    message_content: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[DraftStatus] = mapped_column(
        Enum(DraftStatus, name="draft_status_enum", native_enum=True),
        nullable=False,
        default=DraftStatus.DRAFT,
        index=True
    )
    created_by_user_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True
    )

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic")
    created_by_user: Mapped[Optional["User"]] = relationship("User")
