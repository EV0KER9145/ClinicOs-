import uuid
from typing import List, TYPE_CHECKING
from sqlalchemy import String, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base, UUIDMixin, TimestampMixin

if TYPE_CHECKING:
    from app.models.clinic import Clinic
    from app.models.patient import Patient


class Tag(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "tags"

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False)

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic")
    patients: Mapped[List["Patient"]] = relationship(
        "Patient",
        secondary="patient_tags",
        back_populates="tags"
    )

    def __repr__(self) -> str:
        return f"<Tag id={self.id} name='{self.name}'>"
