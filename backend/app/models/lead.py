import uuid
from datetime import datetime
from enum import Enum as PyEnum
from typing import List, TYPE_CHECKING, Optional
from sqlalchemy import String, Boolean, DateTime, Text, ForeignKey, Enum
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base, UUIDMixin, TimestampMixin

if TYPE_CHECKING:
    from app.models.clinic import Clinic
    from app.models.user import User
    from app.models.patient import Patient
    from app.models.lead_note import LeadNote


class LeadSource(str, PyEnum):
    PHONE = "PHONE"
    WHATSAPP = "WHATSAPP"
    INSTAGRAM = "INSTAGRAM"
    GOOGLE = "GOOGLE"
    REFERRAL = "REFERRAL"
    WALK_IN = "WALK_IN"
    WEBSITE = "WEBSITE"
    OTHER = "OTHER"


class LeadStatus(str, PyEnum):
    NEW = "NEW"
    CONTACTED = "CONTACTED"
    INTERESTED = "INTERESTED"
    APPOINTMENT_BOOKED = "APPOINTMENT_BOOKED"
    CONVERTED = "CONVERTED"
    LOST = "LOST"


class Lead(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "leads"

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    full_name: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    phone: Mapped[Optional[str]] = mapped_column(String(20), nullable=True, index=True)
    email: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    source: Mapped[LeadSource] = mapped_column(
        Enum(LeadSource, name="lead_source_enum", native_enum=True),
        nullable=False,
        default=LeadSource.PHONE,
        index=True
    )
    status: Mapped[LeadStatus] = mapped_column(
        Enum(LeadStatus, name="lead_status_enum", native_enum=True),
        nullable=False,
        default=LeadStatus.NEW,
        index=True
    )
    assigned_user_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
        index=True
    )
    interested_service: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    converted_patient_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("patients.id", ondelete="SET NULL"),
        nullable=True,
        index=True
    )
    converted_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic")
    assigned_user: Mapped[Optional["User"]] = relationship("User", foreign_keys=[assigned_user_id])
    converted_patient: Mapped[Optional["Patient"]] = relationship("Patient", foreign_keys=[converted_patient_id])
    notes_list: Mapped[List["LeadNote"]] = relationship(
        "LeadNote",
        back_populates="lead",
        cascade="all, delete-orphan",
        order_by="desc(LeadNote.created_at)"
    )

    def __repr__(self) -> str:
        return f"<Lead id={self.id} name='{self.full_name}' status='{self.status.value}'>"
