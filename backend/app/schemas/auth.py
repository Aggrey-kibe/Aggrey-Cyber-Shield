"""
Authentication request and response schemas for Aggrey Shield.
"""

import re

from pydantic import BaseModel, ConfigDict, Field, field_validator


PASSWORD_MIN_LENGTH = 10
NAME_MAX_LENGTH = 100
EMAIL_MAX_LENGTH = 254


_EMAIL_REGEX = re.compile(
    r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$"
)


class RegisterRequest(BaseModel):
    """Data required to create a new user account."""

    model_config = ConfigDict(str_strip_whitespace=True)

    email: str = Field(min_length=1, max_length=EMAIL_MAX_LENGTH)
    password: str = Field(min_length=PASSWORD_MIN_LENGTH)
    full_name: str = Field(min_length=2, max_length=NAME_MAX_LENGTH)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not _EMAIL_REGEX.fullmatch(value):
            raise ValueError("Enter a valid email address.")
        return value.lower()

    @field_validator("password")
    @classmethod
    def validate_password(cls, value: str) -> str:
        if not any(char.isupper() for char in value):
            raise ValueError("Include at least one uppercase letter.")
        if not any(char.islower() for char in value):
            raise ValueError("Include at least one lowercase letter.")
        if not any(char.isdigit() for char in value):
            raise ValueError("Include at least one number.")
        return value

    @field_validator("full_name")
    @classmethod
    def validate_full_name(cls, value: str) -> str:
        if len(value) < 2:
            raise ValueError("Enter your full name.")
        return value


class LoginRequest(BaseModel):
    """Credentials used to sign in."""

    model_config = ConfigDict(str_strip_whitespace=True)

    email: str = Field(min_length=1, max_length=EMAIL_MAX_LENGTH)
    password: str = Field(min_length=1)
    mfa_code: str | None = Field(default=None, max_length=8)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not _EMAIL_REGEX.fullmatch(value):
            raise ValueError("Enter a valid email address.")
        return value.lower()

    @field_validator("mfa_code")
    @classmethod
    def validate_mfa_code(cls, value: str | None) -> str | None:
        if value is None or value == "":
            return None

        if not value.isdigit() or not 6 <= len(value) <= 8:
            raise ValueError("Enter a valid MFA code.")

        return value


class VerifyEmailRequest(BaseModel):
    """Email address and six-digit verification code."""

    model_config = ConfigDict(str_strip_whitespace=True)

    email: str = Field(min_length=1, max_length=EMAIL_MAX_LENGTH)
    code: str = Field(min_length=6, max_length=6)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not _EMAIL_REGEX.fullmatch(value):
            raise ValueError("Enter a valid email address.")
        return value.lower()

    @field_validator("code")
    @classmethod
    def validate_code(cls, value: str) -> str:
        if not value.isdigit():
            raise ValueError("Verification code must contain only numbers.")
        return value


class ResendVerificationRequest(BaseModel):
    """Email address for requesting a new verification code."""

    model_config = ConfigDict(str_strip_whitespace=True)

    email: str = Field(min_length=1, max_length=EMAIL_MAX_LENGTH)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not _EMAIL_REGEX.fullmatch(value):
            raise ValueError("Enter a valid email address.")
        return value.lower()


class RefreshTokenRequest(BaseModel):
    """Refresh token submitted to obtain a new access token."""

    refresh_token: str = Field(min_length=1, max_length=4096)


class LogoutRequest(BaseModel):
    """Refresh token submitted when ending a session."""

    refresh_token: str = Field(min_length=1, max_length=4096)


class TokenResponse(BaseModel):
    """Authentication tokens returned after successful authentication."""

    access_token: str
    refresh_token: str
    token_type: str = "bearer"
    expires_in: int


class MessageResponse(BaseModel):
    """Simple API success response."""

    message: str

class EmailRequest(BaseModel):
    """Email address used for password-reset requests."""

    model_config = ConfigDict(str_strip_whitespace=True)

    email: str = Field(min_length=1, max_length=EMAIL_MAX_LENGTH)

    @field_validator("email")
    @classmethod
    def validate_email(cls, value: str) -> str:
        if not _EMAIL_REGEX.fullmatch(value):
            raise ValueError("Enter a valid email address.")
        return value.lower()


class ResetPasswordRequest(BaseModel):
    """Password reset token and replacement password."""

    model_config = ConfigDict(str_strip_whitespace=True)

    token: str = Field(min_length=1, max_length=512)
    new_password: str = Field(min_length=PASSWORD_MIN_LENGTH)

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, value: str) -> str:
        if not any(char.isupper() for char in value):
            raise ValueError("Include at least one uppercase letter.")
        if not any(char.islower() for char in value):
            raise ValueError("Include at least one lowercase letter.")
        if not any(char.isdigit() for char in value):
            raise ValueError("Include at least one number.")
        return value


class ChangePasswordRequest(BaseModel):
    """Current and replacement passwords for an authenticated user."""

    model_config = ConfigDict(str_strip_whitespace=True)

    current_password: str = Field(min_length=1)
    new_password: str = Field(min_length=PASSWORD_MIN_LENGTH)

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, value: str) -> str:
        if not any(char.isupper() for char in value):
            raise ValueError("Include at least one uppercase letter.")
        if not any(char.islower() for char in value):
            raise ValueError("Include at least one lowercase letter.")
        if not any(char.isdigit() for char in value):
            raise ValueError("Include at least one number.")
        return value
