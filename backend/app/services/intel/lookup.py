"""Look up scanned URLs in the loaded threat feeds (database only; no network)."""
from dataclasses import dataclass, field
from datetime import datetime, timezone

from sqlalchemy import and_, or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import ThreatFeedRun, ThreatIndicator
from app.services.intel.feeds import FEEDS
from app.services.intel.normalize import canonical_host, canonical_url


@dataclass(frozen=True)
class IntelHit:
    source: str
    indicator_type: str
    category: str
    confidence: str
    license: str
    first_seen_at: datetime
    last_seen_at: datetime


@dataclass(frozen=True)
class SourceStatus:
    name: str
    last_refreshed_at: datetime
    age_hours: float
    fresh: bool
    indicators: int
    license: str
    attribution: str

    def to_dict(self) -> dict:
        return {
            "name": self.name, "last_refreshed_at": self.last_refreshed_at.isoformat(),
            "age_hours": round(self.age_hours, 1), "fresh": self.fresh, "indicators": self.indicators,
            "license": self.license, "attribution": self.attribution,
        }


@dataclass
class IntelResult:
    checked: bool = False                      # at least one fully-loaded feed exists
    sources: list[SourceStatus] = field(default_factory=list)
    hits: dict[str, list[IntelHit]] = field(default_factory=dict)   # keyed by the input URL text

    @property
    def any_stale(self) -> bool:
        return any(not s.fresh for s in self.sources)


async def source_statuses(db: AsyncSession, now: datetime | None = None) -> list[SourceStatus]:
    """Latest successful FULL refresh per known feed (partial/dev runs do not count)."""
    now = now or datetime.now(timezone.utc)
    runs = (
        await db.execute(
            select(ThreatFeedRun)
            .where(ThreatFeedRun.status == "ok", ThreatFeedRun.partial.is_(False),
                   ThreatFeedRun.source.in_(list(FEEDS)))
            .order_by(ThreatFeedRun.finished_at.desc())
        )
    ).scalars().all()
    latest: dict[str, ThreatFeedRun] = {}
    for run in runs:
        latest.setdefault(run.source, run)
    out = []
    for name, run in latest.items():
        feed = FEEDS[name]
        age = (now - run.finished_at).total_seconds() / 3600
        out.append(SourceStatus(name, run.finished_at, age, age <= feed.max_age_hours,
                                run.accepted, feed.license, feed.attribution))
    return sorted(out, key=lambda s: s.name)


async def check_urls(db: AsyncSession, urls: list[str], now: datetime | None = None) -> IntelResult:
    """Check each input URL (exact URL indicators, and host-level domain indicators)."""
    now = now or datetime.now(timezone.utc)
    result = IntelResult(sources=await source_statuses(db, now))
    result.checked = bool(result.sources)
    if not result.checked or not urls:
        return result

    by_url: dict[str, list[str]] = {}
    by_host: dict[str, list[str]] = {}
    for text in urls:
        cu = canonical_url(text)
        if cu is None:
            continue
        by_url.setdefault(cu.sha256, []).append(text)
        ch = canonical_host(cu.host)
        if ch is not None:
            by_host.setdefault(ch.sha256, []).append(text)

    conditions = []
    if by_url:
        conditions.append(and_(ThreatIndicator.indicator_type == "url", ThreatIndicator.value_sha256.in_([bytes.fromhex(h) for h in by_url])))
    if by_host:
        conditions.append(and_(ThreatIndicator.indicator_type == "domain", ThreatIndicator.value_sha256.in_([bytes.fromhex(h) for h in by_host])))
    if not conditions:
        return result

    rows = (
        await db.execute(
            select(ThreatIndicator).where(ThreatIndicator.expires_at > now, or_(*conditions))
        )
    ).scalars().all()
    for row in rows:
        feed = FEEDS.get(row.source)
        hit = IntelHit(
            source=row.source, indicator_type=row.indicator_type, category=row.category,
            confidence=feed.confidence if feed else "unknown", license=feed.license if feed else "unknown",
            first_seen_at=row.first_seen_at, last_seen_at=row.last_seen_at,
        )
        digest = row.value_sha256.hex()
        matched = by_url.get(digest, []) if row.indicator_type == "url" else by_host.get(digest, [])
        for text in matched:
            result.hits.setdefault(text, []).append(hit)
    return result
