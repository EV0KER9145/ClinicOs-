import uuid
from datetime import date
from typing import List, TYPE_CHECKING, Optional
from sqlalchemy import String, Boolean, Date, Text, ForeignKey, Table, Column
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base, UUIDMixin, TimestampMixin

if TYPE_CHECKING:
    from app.models.clinic import Clinic
    from app.models.tag import Tag
    from app.models.patient_note import PatientNote

# Many-to-Many association table between Patients and Tags
patient_tags = Table(
    "patient_tags",
    Base.metadata,
    Column("patient_id", UUID(as_uuid=True), ForeignKey("patients.id", ondelete="CASCADE"), primary_key=True),
    Column("tag_id", UUID(as_uuid=True), ForeignKey("tags.id", ondelete="CASCADE"), primary_key=True)
)


class Patient(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "patients"

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    full_name: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    phone: Mapped[Optional[str]] = mapped_column(String(20), nullable=True, index=True)
    email: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    date_of_birth: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    gender: Mapped[Optional[str]] = mapped_column(String(20), nullable=True)
    address: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic")
    tags: Mapped[List["Tag"]] = relationship(
        "Tag",
        secondary=patient_tags,
        back_populates="patients"
    )
    notes_list: Mapped[List["PatientNote"]] = relationship(
        "PatientNote",
        back_populates="patient",
        cascade="all, delete-orphan",
        order_by="desc(PatientNote.created_at)"
    )

    def __repr__(self) -> str:
        return f"<Patient id={self.id} name='{self.full_name}' phone='{self.phone}'>"
