"""Create and read scans. Every read is scoped to the owning user."""
import hashlib
import re
import uuid
from urllib.parse import urlsplit

from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import Scan, User
from app.services.scanning.message_analyzer import analyze_message
from app.services.intel.lookup import IntelHit, IntelResult, check_urls
from app.services.scanning.types import LIMITATIONS, Finding, score_from, verdict_for
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


LISTED_WEIGHT = 85         # an exact listed page: strong evidence on its own
LISTED_HOST_WEIGHT = 70    # the whole host is listed: still strong, but broader and likelier to include mistakes


def _listed_weight(hits: list[IntelHit]) -> int:
    return LISTED_WEIGHT if any(h.indicator_type == "url" for h in hits) else LISTED_HOST_WEIGHT


def _listed_finding(hits: list[IntelHit], where: str | None = None) -> Finding:
    evidence = "; ".join(sorted({f"{h.source} (last seen {h.last_seen_at:%Y-%m-%d})" for h in hits}))
    if where:
        evidence = f"{where}: {evidence}"
    exact = any(h.indicator_type == "url" for h in hits)
    return Finding(
        "known_threat_listed", "high", "Listed in a public phishing list",
        ("This exact page address appears on a public list of phishing pages. Others reported or "
         "detected it, so treat it as dangerous. Lists can contain mistakes, but a listing is strong evidence."
         if exact else
         "This website appears on a public list of phishing sites (the whole site is listed, not just "
         "this page). Treat it as dangerous. Lists can contain mistakes, especially for sites that host "
         "other people's content."),
        _listed_weight(hits), evidence[:200],
    )


def _hit_dict(hit: IntelHit) -> dict:
    return {
        "source": hit.source, "indicator_type": hit.indicator_type, "category": hit.category,
        "confidence": hit.confidence, "first_seen_at": hit.first_seen_at.isoformat(),
        "last_seen_at": hit.last_seen_at.isoformat(), "license": hit.license,
    }


def _intel_block(intel: IntelResult, hits: list[IntelHit]) -> dict:
    return {
        "checked": intel.checked,
        "listed": bool(hits),
        "hits": [_hit_dict(h) for h in hits],
        "sources": [src.to_dict() for src in intel.sources],
    }


def _limitations(intel: IntelResult | None) -> list[str]:
    items = list(LIMITATIONS)
    if intel is None:  # nothing with a link was scanned
        return items
    if not intel.checked:
        items.append("No threat lists are loaded on this server yet, so known-dangerous addresses were not checked.")
    elif intel.any_stale:
        oldest = max(s.age_hours for s in intel.sources)
        items.append(f"The threat lists are out of date (last refreshed about {oldest:.0f} hours ago), "
                     "so recent threats may be missing.")
    else:
        newest = min(s.age_hours for s in intel.sources)
        items.append(f"Also checked against {len(intel.sources)} public threat list(s), refreshed about "
                     f"{newest:.0f} hours ago. Such lists hold only a fraction of live threats, so not "
                     "being listed means little.")
    return items


def _result_payload(score, findings, urls, intel_block=None, limitations=None, listed=False) -> dict:
    verdict_id, label, summary, recommendation = verdict_for(score)
    return {
        "verdict_label": label,
        "summary": summary,
        "recommendation": recommendation,
        # "listed": a public feed names it; "limited": pattern-based only
        "confidence": "listed" if listed else "limited",
        "findings": [f.to_dict() for f in findings],
        "urls": urls,
        "intel": intel_block,
        "limitations": limitations or list(LIMITATIONS),
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
    # Stage 1-2: validate, normalise, extract features and apply deterministic rules
    analysis = analyze_url(url)  # may raise InvalidUrl (a client error)
    # Stage 3: threat-intelligence lookup (database only; the URL is never fetched)
    intel = await check_urls(db, [url])
    hits = intel.hits.get(url, [])
    # Stage 4: aggregate the evidence
    findings = list(analysis.findings) + ([_listed_finding(hits)] if hits else [])
    score = score_from(findings)
    payload = _result_payload(
        score, findings, [], _intel_block(intel, hits), _limitations(intel), listed=bool(hits)
    )
    return await _save(
        db, user, "url", _preview_url(analysis.normalized_url), _sha256(url.strip()), score, payload,
    )


async def create_message_scan(db: AsyncSession, user: User, message: str) -> Scan:
    analysis = analyze_message(message)
    url_texts = [u.input for u in analysis.urls]
    intel = await check_urls(db, url_texts) if url_texts else None

    url_dicts, all_hits = [], []
    for u in analysis.urls:
        d = u.to_dict()
        hits = intel.hits.get(u.input, []) if intel else []
        d["listed_in_threat_feed"] = bool(hits)
        if hits:
            all_hits.extend(hits)
            d["findings"].append(_listed_finding(hits).to_dict())
            d["risk_score"] = min(100, d["risk_score"] + _listed_weight(hits))
            d["verdict"], d["verdict_label"], _, _ = verdict_for(d["risk_score"])
        url_dicts.append(d)

    findings = list(analysis.findings)
    score = analysis.score
    if all_hits:
        domain = next((u.registered_domain or u.host for u in analysis.urls if intel.hits.get(u.input)), None)
        findings.append(_listed_finding(all_hits, domain))
        score = min(100, score + _listed_weight(all_hits))
    payload = _result_payload(
        score, findings, url_dicts, _intel_block(intel, all_hits) if intel else None,
        _limitations(intel), listed=bool(all_hits),
    )
    return await _save(
        db, user, "message", _preview_message(message), _sha256(message.strip()), score, payload,
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
        "intel": data.get("intel"),
        "limitations": data["limitations"],
        "input_preview": scan.input_preview,
        "created_at": scan.created_at,
    }
