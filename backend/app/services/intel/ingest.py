"""
Atomic, validated feed ingestion.

All writes for one refresh happen in ONE transaction. The refresh is committed
only if the feed passes health checks (not empty, not mostly invalid, not
suddenly much smaller or larger than the last good run). Otherwise it is rolled
back and the previous data stays exactly as it was. A full run is a snapshot:
entries that disappeared from the feed are removed, so cleaned sites stop being flagged.
"""
import ipaddress
import logging
import uuid
from collections import Counter
from datetime import datetime, timedelta, timezone
from pathlib import Path

from sqlalchemy import delete, select, update
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import ThreatFeedRun, ThreatIndicator
from app.services.intel.feeds import BRAND_DOMAINS, SHARED_ROOTS, Feed
from app.services.intel.normalize import Canon, canonical_host, canonical_url

logger = logging.getLogger("aggreyshield.intel")

BATCH_SIZE = 2000            # 9 columns x 2000 rows stays under asyncpg's 32,767 parameter limit
MAX_REJECT_RATIO = 0.20
MIN_SHRINK_FACTOR = 0.5      # a refresh with less than half the last good size is suspicious
MAX_GROWTH_FACTOR = 3.0      # ...and so is one with more than triple (poisoning flood)


def _is_ip(host: str) -> bool:
    try:
        ipaddress.ip_address(host)
        return True
    except ValueError:
        return False


def _parse(feed: Feed, line: str) -> tuple[Canon | None, str | None]:
    """Return (canonical, None) or (None, reject_reason)."""
    if feed.indicator_type == "url":
        if not line.lower().startswith(("http://", "https://")):
            return None, "not_http"
        canon = canonical_url(line)
        return (canon, None) if canon else (None, "invalid_url")
    canon = canonical_host(line)
    if canon is None:
        return None, "invalid_host"
    if canon.registered_domain is None and not _is_ip(canon.host):
        return None, "no_valid_suffix"   # not a real internet name (e.g. 'foo.1x', 'intranet')
    if canon.registered_domain in BRAND_DOMAINS:
        return None, "protected_brand_domain"
    if canon.host == canon.registered_domain and canon.host in SHARED_ROOTS:
        return None, "protected_shared_root"
    return canon, None


async def _flush(db: AsyncSession, feed: Feed, run_id: uuid.UUID, now: datetime, batch: dict) -> None:
    if not batch:
        return
    expires = now + timedelta(days=feed.ttl_days)
    rows = [
        {
            "source": feed.name, "indicator_type": feed.indicator_type, "value_sha256": bytes.fromhex(sha),
            "registered_domain": domain, "category": feed.category, "first_seen_at": now,
            "last_seen_at": now, "expires_at": expires, "feed_run_id": run_id,
        }
        for sha, domain in batch.items()
    ]
    stmt = pg_insert(ThreatIndicator).values(rows)
    stmt = stmt.on_conflict_do_update(
        constraint="uq_indicator_identity",
        set_={  # first_seen_at is deliberately preserved
            "last_seen_at": stmt.excluded.last_seen_at,
            "expires_at": stmt.excluded.expires_at,
            "feed_run_id": stmt.excluded.feed_run_id,
            "category": stmt.excluded.category,
        },
    )
    await db.execute(stmt)
    batch.clear()


async def _finish(db: AsyncSession, run_id: uuid.UUID, **fields) -> ThreatFeedRun:
    await db.execute(update(ThreatFeedRun).where(ThreatFeedRun.id == run_id).values(**fields))
    await db.commit()
    return (await db.execute(select(ThreatFeedRun).where(ThreatFeedRun.id == run_id))).scalar_one()


async def ingest_file(
    db: AsyncSession, feed: Feed, path: Path, *, sha256: str, size: int,
    limit: int | None = None, now: datetime | None = None,
) -> ThreatFeedRun:
    """Validate and load one downloaded feed file. Returns the (finished) run record."""
    now = now or datetime.now(timezone.utc)
    partial = limit is not None
    run = ThreatFeedRun(
        source=feed.name, status="running", partial=partial, started_at=now,
        bytes_downloaded=size, content_sha256=sha256, details={},
    )
    db.add(run)
    await db.commit()
    run_id = run.id

    previous = (
        await db.execute(
            select(ThreatFeedRun.accepted)
            .where(ThreatFeedRun.source == feed.name, ThreatFeedRun.status == "ok",
                   ThreatFeedRun.partial.is_(False))
            .order_by(ThreatFeedRun.finished_at.desc()).limit(1)
        )
    ).scalar_one_or_none()

    total = accepted = rejected = 0
    reasons: Counter = Counter()
    batch: dict[str, str | None] = {}
    try:
        with path.open("r", encoding="utf-8", errors="replace") as handle:
            for raw in handle:
                line = raw.strip()
                if not line or line.startswith("#"):
                    continue
                if limit is not None and total >= limit:
                    break
                total += 1
                canon, reason = _parse(feed, line)
                if canon is None:
                    rejected += 1
                    reasons[reason] += 1
                    continue
                accepted += 1
                batch[canon.sha256] = canon.registered_domain
                if len(batch) >= BATCH_SIZE:
                    await _flush(db, feed, run_id, now, batch)
        await _flush(db, feed, run_id, now, batch)

        problem = None
        if total == 0:
            problem = "feed contained no entries"
        elif not partial:
            if accepted < feed.min_accepted:
                problem = f"only {accepted} valid entries (expected at least {feed.min_accepted})"
            elif rejected / total > MAX_REJECT_RATIO:
                problem = f"{rejected} of {total} lines were invalid (over {int(MAX_REJECT_RATIO * 100)}%)"
            elif previous and accepted < previous * MIN_SHRINK_FACTOR:
                problem = f"feed shrank from {previous} to {accepted} entries (possible truncation)"
            elif previous and accepted > previous * MAX_GROWTH_FACTOR:
                problem = f"feed grew from {previous} to {accepted} entries (possible flood)"
        if problem:
            await db.rollback()  # previous indicators stay exactly as they were
            logger.warning("feed %s refresh REJECTED: %s", feed.name, problem)
            return await _finish(
                db, run_id, status="rejected", finished_at=datetime.now(timezone.utc), error=problem[:500],
                lines_total=total, accepted=accepted, rejected=rejected, details={"reject_reasons": dict(reasons)},
            )

        removed = 0
        if not partial:  # a full run is a complete snapshot: drop what is no longer listed
            removed = (
                await db.execute(
                    delete(ThreatIndicator).where(
                        ThreatIndicator.source == feed.name, ThreatIndicator.feed_run_id != run_id
                    )
                )
            ).rowcount
        removed += (
            await db.execute(
                delete(ThreatIndicator).where(ThreatIndicator.source == feed.name, ThreatIndicator.expires_at < now)
            )
        ).rowcount
        return await _finish(
            db, run_id, status="ok", finished_at=datetime.now(timezone.utc), lines_total=total,
            accepted=accepted, rejected=rejected, removed=removed, details={"reject_reasons": dict(reasons)},
        )
    except Exception as exc:  # noqa: BLE001 - recorded, previous data preserved
        await db.rollback()
        logger.exception("feed %s refresh failed", feed.name)
        return await _finish(
            db, run_id, status="failed", finished_at=datetime.now(timezone.utc),
            error=f"{type(exc).__name__}: {exc}"[:500], lines_total=total, accepted=accepted, rejected=rejected,
        )
