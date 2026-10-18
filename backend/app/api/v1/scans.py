"""Threat-scanning endpoints (signed-in users only)."""
import uuid

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user
from app.core.rate_limit import SCAN_IP, SCAN_USER, enforce_user, limit_by_ip
from app.db.session import get_db
from app.models import User
from app.schemas.scans import (
    MessageScanRequest,
    ScanListItem,
    ScanListResponse,
    ScanResponse,
    UrlScanRequest,
)
from app.services import scan_service
from app.services.scanning.url_analyzer import InvalidUrl

router = APIRouter(
    prefix="/scans",
    tags=["Scans"],
    dependencies=[Depends(limit_by_ip(SCAN_IP))],
)


def _unprocessable(exc: InvalidUrl) -> HTTPException:
    return HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc))


@router.post("/url", response_model=ScanResponse, status_code=status.HTTP_201_CREATED)
async def scan_url(
    request: UrlScanRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Check a link for phishing signs. The link is analysed, never opened."""
    enforce_user(SCAN_USER, current_user.id)
    try:
        scan = await scan_service.create_url_scan(db, current_user, request.url)
    except InvalidUrl as exc:
        raise _unprocessable(exc) from exc
    return scan_service.scan_to_response(scan)


@router.post("/message", response_model=ScanResponse, status_code=status.HTTP_201_CREATED)
async def scan_message(
    request: MessageScanRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """Check an SMS/email/chat message, including every link inside it."""
    enforce_user(SCAN_USER, current_user.id)
    try:
        scan = await scan_service.create_message_scan(db, current_user, request.message)
    except InvalidUrl as exc:
        raise _unprocessable(exc) from exc
    return scan_service.scan_to_response(scan)


@router.get("", response_model=ScanListResponse)
async def list_my_scans(
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0, le=100_000),
    scan_type: str | None = Query(None, pattern="^(url|message)$"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    """The signed-in user's own scan history, newest first."""
    rows, total = await scan_service.list_scans(db, current_user, limit, offset, scan_type)
    items = [
        ScanListItem(
            id=s.id, scan_type=s.scan_type, risk_score=s.risk_score,
            verdict=s.verdict, verdict_label=s.result["verdict_label"],
            input_preview=s.input_preview, created_at=s.created_at,
        )
        for s in rows
    ]
    return {"items": items, "total": total, "limit": limit, "offset": offset}


@router.get("/{scan_id}", response_model=ScanResponse)
async def get_my_scan(
    scan_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> dict:
    scan = await scan_service.get_scan(db, current_user, scan_id)
    if scan is None:  # also returned for other users' scans: never confirm they exist
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Scan not found.")
    return scan_service.scan_to_response(scan)


@router.delete("/{scan_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_my_scan(
    scan_id: uuid.UUID,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
) -> Response:
    if not await scan_service.delete_scan(db, current_user, scan_id):
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Scan not found.")
    return Response(status_code=status.HTTP_204_NO_CONTENT)
