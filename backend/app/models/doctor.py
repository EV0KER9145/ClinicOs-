import uuid
from typing import TYPE_CHECKING, Optional
from sqlalchemy import String, Boolean, ForeignKey
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.models.base import Base, UUIDMixin, TimestampMixin

if TYPE_CHECKING:
    from app.models.clinic import Clinic
    from app.models.user import User


class Doctor(Base, UUIDMixin, TimestampMixin):
    __tablename__ = "doctors"

    clinic_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("clinics.id", ondelete="RESTRICT"),
        nullable=False,
        index=True
    )
    user_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="SET NULL"),
        nullable=True,
        unique=True,
        index=True
    )
    full_name: Mapped[str] = mapped_column(String(255), nullable=False)
    specialty: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)

    # Relationships
    clinic: Mapped["Clinic"] = relationship("Clinic", back_populates="doctors")
    user: Mapped[Optional["User"]] = relationship("User", back_populates="doctor_profile")

    def __repr__(self) -> str:
        return f"<Doctor id={self.id} name='{self.full_name}' specialty='{self.specialty}'>"
