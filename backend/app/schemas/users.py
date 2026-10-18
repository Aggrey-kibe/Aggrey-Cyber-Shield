"""Schemas for the signed-in user's own profile."""
import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict


class UserResponse(BaseModel):
    """Public profile of the signed-in user. Never includes secrets."""

    model_config = ConfigDict(from_attributes=True)

    id: uuid.UUID
    email: str
    full_name: str
    is_verified: bool
    mfa_enabled: bool
    created_at: datetime
