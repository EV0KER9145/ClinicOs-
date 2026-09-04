import uuid
from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password, create_access_token, decode_access_token
from app.models.user import User, UserRole


# Security Unit Tests
def test_password_hashing_and_verification():
    plain_password = "SecurePassword123!"
    hashed = hash_password(plain_password)

    assert hashed != plain_password
    assert hashed.startswith("$2") or len(hashed) > 20
    assert verify_password(plain_password, hashed) is True
    assert verify_password("WrongPassword123!", hashed) is False


def test_jwt_token_encoding_and_decoding():
    user_id = str(uuid.uuid4())
    clinic_id = str(uuid.uuid4())
    role = UserRole.OWNER.value

    token = create_access_token(subject=user_id, clinic_id=clinic_id, role=role)
    assert isinstance(token, str)

    payload = decode_access_token(token)
    assert payload is not None
    assert payload["sub"] == user_id
    assert payload["clinic_id"] == clinic_id
    assert payload["role"] == role


# Auth Integration API Tests
def test_successful_clinic_registration(client: TestClient, db_session: Session):
    reg_payload = {
        "clinic_name": "SmileCare Dental Clinic",
        "clinic_type": "Dental",
        "clinic_phone": "+91 9876543210",
        "clinic_email": "contact@smilecare.com",
        "full_name": "Dr. Priya Mehta",
        "email": "priya@smilecare.com",
        "password": "Password123!"
    }

    response = client.post("/api/v1/auth/register", json=reg_payload)
    assert response.status_code == 201

    data = response.json()
    assert "clinic" in data
    assert "user" in data
    assert data["clinic"]["name"] == "SmileCare Dental Clinic"
    assert data["user"]["email"] == "priya@smilecare.com"
    assert data["user"]["role"] == "OWNER"

    # Verify password is not in response
    assert "password" not in data["user"]
    assert "password_hash" not in data["user"]

    # Verify user exists in DB and password is hashed
    db_user = db_session.query(User).filter(User.email == "priya@smilecare.com").first()
    assert db_user is not None
    assert db_user.password_hash != "Password123!"
    assert verify_password("Password123!", db_user.password_hash) is True


def test_duplicate_email_registration_rejected(client: TestClient):
    reg_payload = {
        "clinic_name": "SmileCare Clinic",
        "full_name": "Dr. Priya Mehta",
        "email": "priya@smilecare.com",
        "password": "Password123!"
    }

    # First registration
    res1 = client.post("/api/v1/auth/register", json=reg_payload)
    assert res1.status_code == 201

    # Second registration with same email
    res2 = client.post("/api/v1/auth/register", json=reg_payload)
    assert res2.status_code == 409
    assert "already exists" in res2.json()["detail"]


def test_login_and_protected_me_endpoint(client: TestClient):
    # 1. Register
    reg_payload = {
        "clinic_name": "Apex Healthcare",
        "full_name": "Rajesh Owner",
        "email": "rajesh@apex.com",
        "password": "SecretPassword123!"
    }
    reg_res = client.post("/api/v1/auth/register", json=reg_payload)
    assert reg_res.status_code == 201

    # 2. Login with valid credentials
    login_payload = {
        "email": "rajesh@apex.com",
        "password": "SecretPassword123!"
    }
    login_res = client.post("/api/v1/auth/login", json=login_payload)
    assert login_res.status_code == 200

    token_data = login_res.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "bearer"
    access_token = token_data["access_token"]

    # 3. Access protected GET /me endpoint with valid token
    headers = {"Authorization": f"Bearer {access_token}"}
    me_res = client.get("/api/v1/auth/me", headers=headers)
    assert me_res.status_code == 200

    user_data = me_res.json()
    assert user_data["email"] == "rajesh@apex.com"
    assert user_data["full_name"] == "Rajesh Owner"
    assert user_data["role"] == "OWNER"
    assert "password_hash" not in user_data


def test_invalid_login_credentials(client: TestClient):
    reg_payload = {
        "clinic_name": "City Care Clinic",
        "full_name": "Sunita Staff",
        "email": "sunita@citycare.com",
        "password": "ValidPassword123!"
    }
    client.post("/api/v1/auth/register", json=reg_payload)

    # Invalid password
    res1 = client.post("/api/v1/auth/login", json={"email": "sunita@citycare.com", "password": "WrongPassword"})
    assert res1.status_code == 401
    assert "Invalid email or password" in res1.json()["detail"]

    # Non-existent email
    res2 = client.post("/api/v1/auth/login", json={"email": "unknown@citycare.com", "password": "ValidPassword123!"})
    assert res2.status_code == 401
    assert "Invalid email or password" in res2.json()["detail"]


def test_protected_me_endpoint_unauthorized(client: TestClient):
    # Missing authorization header
    res1 = client.get("/api/v1/auth/me")
    assert res1.status_code == 403 or res1.status_code == 401

    # Invalid authorization token
    headers = {"Authorization": "Bearer invalid_junk_token"}
    res2 = client.get("/api/v1/auth/me", headers=headers)
    assert res2.status_code == 401
