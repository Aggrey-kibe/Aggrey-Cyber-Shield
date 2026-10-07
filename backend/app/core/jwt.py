"""
JWT creation and decoding utilities for Aggrey Shield.

Access tokens are short-lived.
Refresh tokens are longer-lived and are distinguished by their token type.
"""

from datetime import datetime, timedelta, timezone
from typing import Any
import uuid

import jwt
from jwt import InvalidTokenError

from app.core.config import settings


ACCESS_TOKEN_TYPE = "access"
REFRESH_TOKEN_TYPE = "refresh"


def _create_token(
    subject: str,
    token_type: str,
    expires_delta: timedelta,
) -> str:
    """Create a signed JWT with standard claims."""
    now = datetime.now(timezone.utc)
    expires_at = now + expires_delta

    payload: dict[str, Any] = {
    "sub": subject,
    "type": token_type,
    "jti": str(uuid.uuid4()),
    "iat": now,
    "exp": expires_at,
}

    return jwt.encode(
        payload,
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )


def create_access_token(subject: str) -> str:
    """Create a short-lived access token."""
    return _create_token(
        subject=subject,
        token_type=ACCESS_TOKEN_TYPE,
        expires_delta=timedelta(
            minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES
        ),
    )


def create_refresh_token(subject: str) -> str:
    """Create a longer-lived refresh token."""
    return _create_token(
        subject=subject,
        token_type=REFRESH_TOKEN_TYPE,
        expires_delta=timedelta(
            days=settings.REFRESH_TOKEN_EXPIRE_DAYS
        ),
    )


def decode_token(token: str) -> dict[str, Any] | None:
    """Decode and validate a JWT.

    Returns the claims when valid, otherwise None.
    """
    try:
        return jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except InvalidTokenError:
        return None


def decode_access_token(token: str) -> dict[str, Any] | None:
    """Decode a token only when it is a valid access token."""
    payload = decode_token(token)

    if payload is None:
        return None

    if payload.get("type") != ACCESS_TOKEN_TYPE:
        return None

    if not payload.get("sub"):
        return None

    return payload


def decode_refresh_token(token: str) -> dict[str, Any] | None:
    """Decode a token only when it is a valid refresh token."""
    payload = decode_token(token)

    if payload is None:
        return None

    if payload.get("type") != REFRESH_TOKEN_TYPE:
        return None

    if not payload.get("sub"):
        return None

    return payload