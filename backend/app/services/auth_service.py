"""
Authentication service for Aggrey Shield.

Keeps registration, email verification, login, password recovery,
password changes, and token business logic outside the API routes.
"""

import logging
import secrets
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.jwt import create_access_token, create_refresh_token
from app.core.security import hash_password, verify_password
from app.models import (
    EmailVerificationCode,
    PasswordResetToken,
    RefreshToken,
    User,
)
from app.schemas.auth import (
    ChangePasswordRequest,
    LoginRequest,
    RegisterRequest,
)
from app.services.email_service import (
    send_password_reset_email,
    send_verification_email,
)


logger = logging.getLogger(__name__)

VERIFICATION_CODE_EXPIRY_MINUTES = 10
VERIFICATION_MAX_ATTEMPTS = 5
VERIFICATION_RESEND_COOLDOWN_SECONDS = 60

PASSWORD_RESET_EXPIRY_MINUTES = 15
PASSWORD_RESET_RESEND_COOLDOWN_SECONDS = 60


class AuthenticationError(Exception):
    """Raised when authentication credentials are invalid."""


class RegistrationError(Exception):
    """Raised when registration cannot be completed."""


class EmailVerificationError(Exception):
    """Raised when email verification cannot be completed."""


class PasswordResetError(Exception):
    """Raised when password recovery cannot be completed."""


def generate_verification_code() -> str:
    """Generate a cryptographically secure six-digit verification code."""
    return str(secrets.randbelow(900000) + 100000)


async def create_verification_code(
    db: AsyncSession,
    user: User,
) -> str:
    """
    Create a new email verification code.

    The transaction is intentionally not committed here.
    The caller commits only after email delivery succeeds.
    """

    now = datetime.now(timezone.utc)

    await db.execute(
        update(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id,
            EmailVerificationCode.used_at.is_(None),
        )
        .values(used_at=now)
    )

    code = generate_verification_code()

    verification = EmailVerificationCode(
        user_id=user.id,
        code_hash=EmailVerificationCode.hash_code(code),
        expires_at=now + timedelta(
            minutes=VERIFICATION_CODE_EXPIRY_MINUTES
        ),
        attempts=0,
    )

    db.add(verification)

    return code


async def register_user(
    db: AsyncSession,
    request: RegisterRequest,
) -> User:
    """Create a new user account and send an email verification code."""

    existing_result = await db.execute(
        select(User).where(User.email == request.email)
    )
    existing_user = existing_result.scalar_one_or_none()

    if existing_user is not None:
        raise RegistrationError(
            "An account with that email already exists."
        )

    user = User(
        email=request.email,
        full_name=request.full_name,
        password_hash=hash_password(request.password),
        is_active=True,
        is_verified=False,
        mfa_enabled=False,
    )

    db.add(user)
    await db.flush()

    try:
        code = await create_verification_code(db, user)

        send_verification_email(
            user.email,
            code,
        )

        await db.commit()

    except Exception as exc:
        await db.rollback()
        logger.exception(
            "Registration email delivery or transaction failed"
        )

        raise RegistrationError(
            "We could not send the verification email. Please try again later."
        ) from exc

    await db.refresh(user)

    return user


async def verify_user_email(
    db: AsyncSession,
    email: str,
    code: str,
) -> None:
    """Verify a user's email using a valid six-digit code."""

    result = await db.execute(
        select(User).where(User.email == email)
    )
    user = result.scalar_one_or_none()

    if user is None:
        raise EmailVerificationError(
            "Invalid verification code."
        )

    if user.is_verified:
        raise EmailVerificationError(
            "This email is already verified."
        )

    now = datetime.now(timezone.utc)

    result = await db.execute(
        select(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id,
            EmailVerificationCode.used_at.is_(None),
        )
        .order_by(
            EmailVerificationCode.created_at.desc()
        )
    )

    verification = result.scalars().first()

    if verification is None:
        raise EmailVerificationError(
            "Invalid or expired verification code."
        )

    if verification.expires_at <= now:
        raise EmailVerificationError(
            "This verification code has expired."
        )

    if verification.attempts >= VERIFICATION_MAX_ATTEMPTS:
        raise EmailVerificationError(
            "Too many incorrect attempts. Please request a new code."
        )

    if verification.code_hash != EmailVerificationCode.hash_code(code):
        verification.attempts += 1
        await db.commit()

        raise EmailVerificationError(
            "Invalid verification code."
        )

    verification.used_at = now
    user.is_verified = True

    await db.commit()


async def resend_verification_code(
    db: AsyncSession,
    email: str,
) -> None:
    """
    Create and send a new email verification code.

    The previous active code is invalidated only as part of the same
    transaction. The transaction is committed only after the email
    has been successfully accepted by the email service.
    """

    result = await db.execute(
        select(User).where(User.email == email)
    )
    user = result.scalar_one_or_none()

    if user is None:
        raise EmailVerificationError(
            "If an account exists for this email, a verification code will be sent."
        )

    if user.is_verified:
        raise EmailVerificationError(
            "This email is already verified."
        )

    now = datetime.now(timezone.utc)

    result = await db.execute(
        select(EmailVerificationCode)
        .where(
            EmailVerificationCode.user_id == user.id
        )
        .order_by(
            EmailVerificationCode.created_at.desc()
        )
    )

    latest_code = result.scalars().first()

    if latest_code is not None:
        seconds_since_last_code = (
            now - latest_code.created_at
        ).total_seconds()

        if (
            seconds_since_last_code
            < VERIFICATION_RESEND_COOLDOWN_SECONDS
        ):
            remaining = int(
                VERIFICATION_RESEND_COOLDOWN_SECONDS
                - seconds_since_last_code
            )

            raise EmailVerificationError(
                f"Please wait {max(1, remaining)} seconds "
                "before requesting another code."
            )

    try:
        code = await create_verification_code(
            db,
            user,
        )

        send_verification_email(
            user.email,
            code,
        )

        await db.commit()

    except Exception as exc:
        await db.rollback()

        raise EmailVerificationError(
            "We could not send the verification email. Please try again later."
        ) from exc


async def authenticate_user(
    db: AsyncSession,
    request: LoginRequest,
) -> User:
    """Validate login credentials and return the authenticated user."""

    result = await db.execute(
        select(User).where(User.email == request.email)
    )
    user = result.scalar_one_or_none()

    if user is None or not verify_password(
        request.password,
        user.password_hash,
    ):
        raise AuthenticationError(
            "Invalid email or password."
        )

    if not user.is_active:
        raise AuthenticationError(
            "This account is inactive."
        )

    if not user.is_verified:
        raise AuthenticationError(
            "Please verify your email address before signing in."
        )

    if user.mfa_enabled:
        if not request.mfa_code:
            raise AuthenticationError(
                "MFA code required."
            )

        raise AuthenticationError(
            "MFA verification is not configured yet."
        )

    return user


async def create_user_tokens(
    db: AsyncSession,
    user: User,
) -> tuple[str, str]:
    """Create and persist an access-token and refresh-token pair."""

    subject = str(user.id)

    access_token = create_access_token(subject)
    refresh_token = create_refresh_token(subject)

    expires_at = (
        datetime.now(timezone.utc)
        + timedelta(
            days=settings.REFRESH_TOKEN_EXPIRE_DAYS
        )
    )

    stored_refresh_token = RefreshToken(
        user_id=user.id,
        token_hash=RefreshToken.hash_token(
            refresh_token
        ),
        expires_at=expires_at,
        revoked=False,
    )

    db.add(stored_refresh_token)

    await db.commit()

    return access_token, refresh_token


async def refresh_user_tokens(
    db: AsyncSession,
    refresh_token: str,
) -> tuple[str, str]:
    """Validate, rotate, and persist a refresh-token pair."""

    from app.core.jwt import decode_refresh_token

    payload = decode_refresh_token(refresh_token)

    if payload is None:
        raise AuthenticationError(
            "Invalid or expired refresh token."
        )

    subject = payload.get("sub")

    try:
        user_id = uuid.UUID(str(subject))
    except (ValueError, TypeError) as exc:
        raise AuthenticationError(
            "Invalid refresh token."
        ) from exc

    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash
            == RefreshToken.hash_token(refresh_token)
        )
    )

    stored_token = result.scalar_one_or_none()

    if stored_token is None or stored_token.revoked:
        raise AuthenticationError(
            "Invalid or revoked refresh token."
        )

    if stored_token.expires_at <= datetime.now(timezone.utc):
        raise AuthenticationError(
            "Refresh token has expired."
        )

    result = await db.execute(
        select(User).where(User.id == user_id)
    )

    user = result.scalar_one_or_none()

    if user is None or not user.is_active:
        raise AuthenticationError(
            "User account is unavailable."
        )

    stored_token.revoked = True
    stored_token.revoked_at = datetime.now(timezone.utc)

    access_token = create_access_token(str(user.id))
    new_refresh_token = create_refresh_token(str(user.id))

    expires_at = (
        datetime.now(timezone.utc)
        + timedelta(
            days=settings.REFRESH_TOKEN_EXPIRE_DAYS
        )
    )

    new_stored_refresh_token = RefreshToken(
        user_id=user.id,
        token_hash=RefreshToken.hash_token(
            new_refresh_token
        ),
        expires_at=expires_at,
        revoked=False,
    )

    db.add(new_stored_refresh_token)

    try:
        await db.commit()
    except Exception as exc:
        await db.rollback()

        raise AuthenticationError(
            "Could not refresh the session. Please sign in again."
        ) from exc

    return access_token, new_refresh_token


async def logout_user(
    db: AsyncSession,
    refresh_token: str,
) -> None:
    """Revoke a refresh token during logout."""

    token_hash = RefreshToken.hash_token(
        refresh_token
    )

    result = await db.execute(
        select(RefreshToken).where(
            RefreshToken.token_hash == token_hash
        )
    )

    stored_token = result.scalar_one_or_none()

    if stored_token is None:
        raise AuthenticationError(
            "Invalid or revoked refresh token."
        )

    if stored_token.revoked:
        raise AuthenticationError(
            "Invalid or revoked refresh token."
        )

    stored_token.revoked = True
    stored_token.revoked_at = datetime.now(timezone.utc)

    await db.commit()


def generate_password_reset_token() -> str:
    """Generate a cryptographically secure password-reset token."""
    return secrets.token_urlsafe(48)


async def request_password_reset(
    db: AsyncSession,
    email: str,
) -> None:
    """
    Create and email a password-reset token.

    The caller should always return a generic response so the API does
    not reveal whether an email address belongs to an account.
    """

    result = await db.execute(
        select(User).where(
            User.email == email,
            User.is_active.is_(True),
        )
    )
    user = result.scalar_one_or_none()

    if user is None:
        return

    now = datetime.now(timezone.utc)

    result = await db.execute(
        select(PasswordResetToken)
        .where(
            PasswordResetToken.user_id == user.id
        )
        .order_by(
            PasswordResetToken.created_at.desc()
        )
    )

    latest_token = result.scalars().first()

    if latest_token is not None:
        seconds_since_last_request = (
            now - latest_token.created_at
        ).total_seconds()

        if (
            seconds_since_last_request
            < PASSWORD_RESET_RESEND_COOLDOWN_SECONDS
        ):
            return

    await db.execute(
        update(PasswordResetToken)
        .where(
            PasswordResetToken.user_id == user.id,
            PasswordResetToken.used_at.is_(None),
        )
        .values(used_at=now)
    )

    raw_token = generate_password_reset_token()

    reset_token = PasswordResetToken(
        user_id=user.id,
        token_hash=PasswordResetToken.hash_token(
            raw_token
        ),
        expires_at=now + timedelta(
            minutes=PASSWORD_RESET_EXPIRY_MINUTES
        ),
    )

    db.add(reset_token)

    try:
        await db.flush()

        send_password_reset_email(
            user.email,
            raw_token,
        )

        await db.commit()

    except Exception as exc:
        await db.rollback()

        logger.exception(
            "Password reset email delivery or transaction failed"
        )

        raise PasswordResetError(
            "We could not process the password reset request."
        ) from exc


async def reset_password(
    db: AsyncSession,
    token: str,
    new_password: str,
) -> None:
    """Reset a user's password using a valid, single-use reset token."""

    token_hash = PasswordResetToken.hash_token(token)

    result = await db.execute(
        select(PasswordResetToken).where(
            PasswordResetToken.token_hash == token_hash
        )
    )

    reset_token = result.scalar_one_or_none()

    if reset_token is None:
        raise PasswordResetError(
            "Invalid or expired password reset link."
        )

    now = datetime.now(timezone.utc)

    if reset_token.used_at is not None:
        raise PasswordResetError(
            "This password reset link has already been used."
        )

    if reset_token.expires_at <= now:
        raise PasswordResetError(
            "This password reset link has expired."
        )

    result = await db.execute(
        select(User).where(User.id == reset_token.user_id)
    )

    user = result.scalar_one_or_none()

    if user is None or not user.is_active:
        raise PasswordResetError(
            "This password reset link is invalid."
        )

    if verify_password(new_password, user.password_hash):
        raise PasswordResetError(
            "The new password must be different from your current password."
        )

    user.password_hash = hash_password(new_password)
    reset_token.used_at = now

    await db.execute(
        update(PasswordResetToken)
        .where(
            PasswordResetToken.user_id == user.id,
            PasswordResetToken.id != reset_token.id,
            PasswordResetToken.used_at.is_(None),
        )
        .values(used_at=now)
    )

    await db.execute(
        update(RefreshToken)
        .where(
            RefreshToken.user_id == user.id,
            RefreshToken.revoked.is_(False),
        )
        .values(
            revoked=True,
            revoked_at=now,
        )
    )

    try:
        await db.commit()
    except Exception as exc:
        await db.rollback()

        raise PasswordResetError(
            "Could not reset the password. Please try again."
        ) from exc


async def change_password(
    db: AsyncSession,
    user: User,
    request: ChangePasswordRequest,
) -> None:
    """Change the authenticated user's password."""

    if not verify_password(
        request.current_password,
        user.password_hash,
    ):
        raise PasswordResetError(
            "Current password is incorrect."
        )

    if verify_password(
        request.new_password,
        user.password_hash,
    ):
        raise PasswordResetError(
            "The new password must be different from your current password."
        )

    now = datetime.now(timezone.utc)

    user.password_hash = hash_password(
        request.new_password
    )

    await db.execute(
        update(RefreshToken)
        .where(
            RefreshToken.user_id == user.id,
            RefreshToken.revoked.is_(False),
        )
        .values(
            revoked=True,
            revoked_at=now,
        )
    )

    try:
        await db.commit()
    except Exception as exc:
        await db.rollback()

        raise PasswordResetError(
            "Could not change the password. Please try again."
        ) from exc
