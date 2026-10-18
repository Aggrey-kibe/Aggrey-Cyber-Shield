"""The signed-in user's own profile."""
from fastapi import APIRouter, Depends

from app.core.dependencies import get_current_user
from app.models import User
from app.schemas.users import UserResponse

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=UserResponse)
async def read_me(current_user: User = Depends(get_current_user)) -> User:
    """Who am I? Returns the profile of the user who owns the access token."""
    return current_user
