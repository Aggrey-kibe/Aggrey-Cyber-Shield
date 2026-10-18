"""Create and read scans. Every read is scoped to the owning user."""
import hashlib
import re
import uuid
from urllib.parse import urlsplit

from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Scan, User
from app.services.scanning.message_analyzer import analyze_message
from app.services.scanning.types import LIMITATIONS, Finding, verdict_for
from app.services.scanning.url_analyzer import analyze_url

__all__ = [
    "create_url_scan", "create_message_scan", "list_scans", "get_scan",
    "delete_scan", "scan_to_response",
]

_DIGIT_RUN = re.compile(r"\d{4,}")


def _sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", "replace")).hexdigest()


def _preview_url(normalized_url: str) -> str:
    """Scheme, host and path only: queries and fragments often hold tokens."""
    try:
        parts = urlsplit(normalized_url)
        shown = f"{parts.scheme}://{parts.netloc}{parts.path}" if parts.netloc else normalized_url
    except ValueError:
        shown = normalized_url.split("?", 1)[0]
    return shown[:300]


def _preview_message(text: str) -> str:
    """Short preview with long digit runs (OTPs, phone/account numbers) masked."""
    flat = " ".join(text.split())
    masked = _DIGIT_RUN.sub(lambda m: "•" * len(m.group(0)), flat)
    return masked[:100] + ("…" if len(masked) > 100 else "")


def _result_payload(score: int, findings: list[Finding], urls: list[dict]) -> dict:
    verdict_id, label, summary, recommendation = verdict_for(score)
    return {
        "verdict_label": label,
        "summary": summary,
        "recommendation": recommendation,
        "confidence": "limited",  # pattern-based only; no live reputation data
        "findings": [f.to_dict() for f in findings],
        "urls": urls,
        "limitations": list(LIMITATIONS),
    }


async def _save(db, user, scan_type, preview, digest, score, payload) -> Scan:
    scan = Scan(
        user_id=user.id,
        scan_type=scan_type,
        input_preview=preview,
        input_sha256=digest,
        risk_score=score,
        verdict=verdict_for(score)[0],
        result=payload,
    )
    db.add(scan)
    await db.commit()
    await db.refresh(scan)
    return scan


async def create_url_scan(db: AsyncSession, user: User, url: str) -> Scan:
    analysis = analyze_url(url)  # may raise InvalidUrl (a client error)
    payload = _result_payload(analysis.score, analysis.findings, [])
    return await _save(
        db, user, "url", _preview_url(analysis.normalized_url),
        _sha256(url.strip()), analysis.score, payload,
    )


async def create_message_scan(db: AsyncSession, user: User, message: str) -> Scan:
    analysis = analyze_message(message)
    payload = _result_payload(
        analysis.score, analysis.findings, [u.to_dict() for u in analysis.urls]
    )
    return await _save(
        db, user, "message", _preview_message(message),
        _sha256(message.strip()), analysis.score, payload,
    )


async def list_scans(
    db: AsyncSession, user: User, limit: int, offset: int, scan_type: str | None = None
) -> tuple[list[Scan], int]:
    conditions = [Scan.user_id == user.id]
    if scan_type:
        conditions.append(Scan.scan_type == scan_type)
    total = (
        await db.execute(select(func.count()).select_from(Scan).where(*conditions))
    ).scalar_one()
    rows = (
        await db.execute(
            select(Scan).where(*conditions)
            .order_by(Scan.created_at.desc(), Scan.id)
            .limit(limit).offset(offset)
        )
    ).scalars().all()
    return list(rows), total


async def get_scan(db: AsyncSession, user: User, scan_id: uuid.UUID) -> Scan | None:
    return (
        await db.execute(
            select(Scan).where(Scan.id == scan_id, Scan.user_id == user.id)
        )
    ).scalar_one_or_none()


async def delete_scan(db: AsyncSession, user: User, scan_id: uuid.UUID) -> bool:
    result = await db.execute(
        delete(Scan).where(Scan.id == scan_id, Scan.user_id == user.id)
    )
    await db.commit()
    return result.rowcount > 0


def scan_to_response(scan: Scan) -> dict:
    data = scan.result
    return {
        "id": scan.id,
        "scan_type": scan.scan_type,
        "risk_score": scan.risk_score,
        "verdict": scan.verdict,
        "verdict_label": data["verdict_label"],
        "summary": data["summary"],
        "recommendation": data["recommendation"],
        "confidence": data["confidence"],
        "findings": data["findings"],
        "urls": data.get("urls", []),
        "limitations": data["limitations"],
        "input_preview": scan.input_preview,
        "created_at": scan.created_at,
    }
