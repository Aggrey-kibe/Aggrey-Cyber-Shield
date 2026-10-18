"""Threat-list status (so apps can show how fresh the protection data is)."""
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user
from app.core.rate_limit import SCAN_IP, limit_by_ip
from app.db.session import get_db
from app.models import User
from app.schemas.scans import IntelSourceOut
from app.services.intel.lookup import source_statuses

router = APIRouter(prefix="/intel", tags=["Threat intelligence"], dependencies=[Depends(limit_by_ip(SCAN_IP))])


class IntelStatusResponse(BaseModel):
    checked: bool
    sources: list[IntelSourceOut]


@router.get("/status", response_model=IntelStatusResponse)
async def intel_status(
    current_user: User = Depends(get_current_user), db: AsyncSession = Depends(get_db)
) -> dict:
    """Which threat lists are loaded, how fresh they are, and under what licence."""
    sources = await source_statuses(db)
    return {"checked": bool(sources), "sources": [s.to_dict() for s in sources]}
