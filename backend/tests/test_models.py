import uuid
from app.models import Base, Clinic, User, UserRole, Doctor
from app.schemas import ClinicCreate, ClinicResponse, UserCreate, UserResponse, DoctorCreate, DoctorResponse


def test_sqlalchemy_metadata_tables():
    tables = list(Base.metadata.tables.keys())
    assert "clinics" in tables
    assert "users" in tables
    assert "doctors" in tables


def test_clinic_model_instantiation():
    clinic = Clinic(name="SmileCare Clinic", clinic_type="Dental", timezone="Asia/Kolkata", is_active=True)
    assert clinic.name == "SmileCare Clinic"
    assert clinic.clinic_type == "Dental"
    assert clinic.timezone == "Asia/Kolkata"
    assert clinic.is_active is True


def test_user_model_instantiation():
    clinic_id = uuid.uuid4()
    user = User(
        clinic_id=clinic_id,
        full_name="Dr. Priya Mehta",
        email="priya@smilecare.com",
        password_hash="hashed_secret",
        role=UserRole.DOCTOR,
        is_active=True
    )
    assert user.clinic_id == clinic_id
    assert user.full_name == "Dr. Priya Mehta"
    assert user.role == UserRole.DOCTOR
    assert user.is_active is True


def test_doctor_model_instantiation():
    clinic_id = uuid.uuid4()
    user_id = uuid.uuid4()
    doctor = Doctor(
        clinic_id=clinic_id,
        user_id=user_id,
        full_name="Dr. Priya Mehta",
        specialty="Dentist",
        is_active=True
    )
    assert doctor.clinic_id == clinic_id
    assert doctor.user_id == user_id
    assert doctor.full_name == "Dr. Priya Mehta"
    assert doctor.specialty == "Dentist"


def test_pydantic_schemas_validation():
    clinic_in = ClinicCreate(
        name="Apex Health Clinic",
        email="info@apexhealth.com",
        phone="+91 9876543210"
    )
    assert clinic_in.name == "Apex Health Clinic"
    assert clinic_in.timezone == "Asia/Kolkata"
    assert clinic_in.is_active is True

    user_in = UserCreate(
        full_name="Rajesh Admin",
        email="admin@apexhealth.com",
        password="secretpassword123",
        role=UserRole.ADMIN
    )
    assert user_in.full_name == "Rajesh Admin"
    assert user_in.role == UserRole.ADMIN

    # Verify UserResponse schema excludes password / password_hash
    user_resp_fields = UserResponse.model_fields.keys()
    assert "password" not in user_resp_fields
    assert "password_hash" not in user_resp_fields
