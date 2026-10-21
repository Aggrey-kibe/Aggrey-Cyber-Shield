"""One-word summary of how healthy the threat lists are (for /api/v1/health and monitors)."""
from sqlalchemy.ext.asyncio import AsyncSession

from app.services.intel.feeds import FEEDS
from app.services.intel.lookup import source_statuses


async def intel_health(db: AsyncSession) -> dict:
    """
    status:
      not_loaded  no feed has ever been fully loaded: known-bad lookups are NOT happening
      stale       at least one loaded feed is older than its freshness limit
      incomplete  everything loaded is fresh, but not every configured feed is loaded
      fresh       every configured feed is loaded and within its freshness limit
    """
    statuses = await source_statuses(db)
    if not statuses:
        status = "not_loaded"
    elif any(not s.fresh for s in statuses):
        status = "stale"
    elif len(statuses) < len(FEEDS):
        status = "incomplete"
    else:
        status = "fresh"
    return {
        "status": status,
        "sources_loaded": len(statuses),
        "sources_expected": len(FEEDS),
        "oldest_refresh_age_hours": round(max(s.age_hours for s in statuses), 1) if statuses else None,
    }
