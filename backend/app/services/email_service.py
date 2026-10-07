"""Email delivery service for Aggrey Shield."""

import resend

from app.core.config import settings


def send_verification_email(
    recipient_email: str,
    verification_code: str,
) -> None:
    """Send an email verification code through Resend."""

    resend.api_key = settings.RESEND_API_KEY

    resend.Emails.send(
        {
            "from": "AGGREY SHIELD <onboarding@resend.dev>",
            "to": [recipient_email],
            "subject": "Verify your AGGREY SHIELD account",
            "html": f"""
                <div style="font-family: Arial, sans-serif; max-width: 600px;">
                    <h2>Welcome to AGGREY SHIELD</h2>
                    <p>Use the verification code below to verify your email address:</p>

                    <div style="
                        font-size: 32px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        margin: 24px 0;
                    ">
                        {verification_code}
                    </div>

                    <p>This code expires in 10 minutes.</p>
                    <p>If you did not create an AGGREY SHIELD account, you can ignore this email.</p>

                    <p>
                        Regards,<br>
                        AGGREY SHIELD Security Team
                    </p>
                </div>
            """,
        }
    )


def send_password_reset_email(
    recipient_email: str,
    reset_token: str,
) -> None:
    """Send a password-reset link through Resend."""

    resend.api_key = settings.RESEND_API_KEY

    reset_link = (
        f"{settings.PASSWORD_RESET_URL_BASE}?token="
        f"{reset_token}"
    )
    resend.Emails.send(
        {
            "from": "AGGREY SHIELD <onboarding@resend.dev>",
            "to": [recipient_email],
            "subject": "Reset your AGGREY SHIELD password",
            "html": f"""
                <div style="font-family: Arial, sans-serif; max-width: 600px;">
                    <h2>Password Reset Request</h2>

                    <p>
                        We received a request to reset your AGGREY SHIELD password.
                    </p>

                    <p>
                        Use the secure reset link below to create a new password:
                    </p>

                    <p style="margin: 24px 0;">
                        <a
                            href="{reset_link}"
                            style="
                                display: inline-block;
                                padding: 12px 20px;
                                background: #111827;
                                color: #ffffff;
                                text-decoration: none;
                                border-radius: 6px;
                            "
                        >
                            Reset Password
                        </a>
                    </p>

                    <p>
                        This reset link expires in 15 minutes and can only be used once.
                    </p>

                    <p>
                        If you did not request a password reset, you can safely ignore
                        this email.
                    </p>

                    <p>
                        Regards,<br>
                        AGGREY SHIELD Security Team
                    </p>
                </div>
            """,
        }
    )
