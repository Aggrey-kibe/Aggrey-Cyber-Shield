import asyncio

from app.core.jwt import decode_access_token, decode_refresh_token
from app.core.security import hash_password
from app.db.session import AsyncSessionLocal
from app.models import RefreshToken, User
from app.schemas.auth import LoginRequest
from app.services.auth_service import authenticate_user, create_user_tokens
from sqlalchemy import select


async def main():
    async with AsyncSessionLocal() as db:
        result = await db.execute(
            select(User).where(User.email == "auth-test@example.com")
        )
        user = result.scalar_one()

        access_token, refresh_token = await create_user_tokens(db, user)

        print("Login token creation successful:", user.email)
        print(
            "Access token valid:",
            decode_access_token(access_token) is not None,
        )
        print(
            "Refresh token valid:",
            decode_refresh_token(refresh_token) is not None,
        )

        result = await db.execute(
            select(RefreshToken).where(
                RefreshToken.token_hash == RefreshToken.hash_token(refresh_token)
            )
        )
        stored_token = result.scalar_one_or_none()

        print(
            "Refresh token stored in database:",
            stored_token is not None,
        )


asyncio.run(main())