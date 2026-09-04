from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional
import jwt
from pwdlib import PasswordHash
from pwdlib.hashers.bcrypt import BcryptHasher
from app.core.config import settings

# Configure password hasher using bcrypt via pwdlib
password_hasher = PasswordHash((BcryptHasher(),))


def hash_password(password: str) -> str:
    """Securely hash a plain-text password using bcrypt."""
    return password_hasher.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify a plain-text password against a hashed password."""
    return password_hasher.verify(plain_password, hashed_password)


def create_access_token(
    subject: str,
    clinic_id: str,
    role: str,
    expires_delta: Optional[timedelta] = None
) -> str:
    """Generate a signed JWT access token containing subject and tenant claims."""
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)

    to_encode: Dict[str, Any] = {
        "sub": str(subject),
        "clinic_id": str(clinic_id),
        "role": str(role),
        "exp": expire,
        "iat": datetime.now(timezone.utc)
    }

    encoded_jwt = jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)
    return encoded_jwt


def decode_access_token(token: str) -> Optional[Dict[str, Any]]:
    """Decode and validate a JWT access token."""
    try:
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM]
        )
        return payload
    except (jwt.PyJWTError, ValueError):
        return None
