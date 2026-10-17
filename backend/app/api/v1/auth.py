"""
Authentication API routes for Aggrey Shield.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user
from app.db.session import get_db
from app.models import User
from app.schemas.auth import (
    ChangePasswordRequest,
    EmailRequest,
    LoginRequest,
    LogoutRequest,
    MessageResponse,
    RefreshTokenRequest,
    RegisterRequest,
    ResendVerificationRequest,
    ResetPasswordRequest,
    TokenResponse,
    VerifyEmailRequest,
)
from app.services.auth_service import (
    AuthenticationError,
    EmailDeliveryError,
    EmailVerificationError,
    PasswordResetError,
    RegistrationError,
    authenticate_user,
    change_password,
    create_user_tokens,
    logout_user,
    refresh_user_tokens,
    register_user,
    request_password_reset,
    reset_password,
    resend_verification_code,
    verify_user_email,
)

router = APIRouter(
    prefix="/auth",
    tags=["Authentication"],
)


@router.post(
    "/register",
    response_model=MessageResponse,
    status_code=status.HTTP_201_CREATED,
)
async def register(
    request: RegisterRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Register a new Aggrey Shield user."""

    try:
        await register_user(db, request)
    except RegistrationError as exc:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(exc),
        ) from exc
    except EmailDeliveryError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        message="Account created successfully. Please check your email for the verification code.",
    )


@router.post("/verify-email", response_model=MessageResponse)
async def verify_email(
    request: VerifyEmailRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Verify a user's email address."""

    try:
        await verify_user_email(
            db,
            request.email,
            request.code,
        )
    except EmailVerificationError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        message="Email verified successfully. You can now sign in.",
    )


@router.post(
    "/resend-verification",
    response_model=MessageResponse,
)
async def resend_verification(
    request: ResendVerificationRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Send a new email verification code."""

    try:
        await resend_verification_code(
            db,
            request.email,
        )
    except EmailVerificationError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    except EmailDeliveryError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        message="If the account exists and is not verified, a new verification code has been sent.",
    )


@router.post(
    "/forgot-password",
    response_model=MessageResponse,
)
async def forgot_password(
    request: EmailRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """
    Start password recovery.

    The response is intentionally identical whether or not the account
    exists, preventing email-account enumeration.
    """

    try:
        await request_password_reset(
            db,
            request.email,
        )
    except PasswordResetError:
        # Do not reveal whether an account exists or expose email-service
        # details through the password-recovery endpoint.
        pass

    return MessageResponse(
        message="If an account exists for that email, password reset instructions have been sent.",
    )


@router.post(
    "/reset-password",
    response_model=MessageResponse,
)
async def reset_password_route(
    request: ResetPasswordRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Reset a password using a valid single-use reset token."""

    try:
        await reset_password(
            db,
            request.token,
            request.new_password,
        )
    except PasswordResetError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        message="Password reset successfully. Please sign in with your new password.",
    )


@router.post(
    "/change-password",
    response_model=MessageResponse,
)
async def change_password_route(
    request: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Change the authenticated user's password."""

    try:
        await change_password(
            db,
            current_user,
            request,
        )
    except PasswordResetError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    return MessageResponse(
        message="Password changed successfully. Please sign in again.",
    )


@router.post("/login")
async def login(
    request: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """Authenticate a user and return access and refresh tokens."""

    try:
        user = await authenticate_user(db, request)
    except AuthenticationError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(exc),
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc

    access_token, refresh_token = await create_user_tokens(db, user)

    return {
        "access_token": access_token,
        "refresh_token": refresh_token,
        "token_type": "bearer",
        "expires_in": 30 * 60,
    }


@router.post("/refresh", response_model=TokenResponse)
async def refresh(
    request: RefreshTokenRequest,
    db: AsyncSession = Depends(get_db),
) -> TokenResponse:
    """Refresh an access token using a valid refresh token."""

    try:
        access_token, refresh_token = await refresh_user_tokens(
            db,
            request.refresh_token,
        )
    except AuthenticationError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(exc),
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer",
        expires_in=30 * 60,
    )


@router.post(
    "/logout",
    response_model=MessageResponse,
)
async def logout(
    request: LogoutRequest,
    db: AsyncSession = Depends(get_db),
) -> MessageResponse:
    """Revoke the supplied refresh token."""

    try:
        await logout_user(db, request.refresh_token)
    except AuthenticationError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(exc),
            headers={"WWW-Authenticate": "Bearer"},
        ) from exc

    return MessageResponse(
        message="Logged out successfully.",
    )
