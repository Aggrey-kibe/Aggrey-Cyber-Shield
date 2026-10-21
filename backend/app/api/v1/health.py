"""
Versioned readiness endpoint.

GET /api/v1/health checks both the API process and live database
connectivity, and returns HTTP 503 if the database is unreachable so that
orchestrators (ECS/ALB target groups) stop routing traffic to this instance.

It also reports how fresh the threat lists are (`threat_intel`). Stale or missing
lists do NOT make the service unready: it still works, with weaker protection. The
field exists so an operator or monitor can alert on it instead of the protection
quietly degrading.

This is distinct from the plain GET /health liveness endpoint defined in
main.py, which has zero dependencies and is used for fast container restarts
decisions rather than traffic routing decisions.
"""
import logging
import time

from fastapi import APIRouter, Response, status

from app.core.config import settings
from app.db.session import AsyncSessionLocal, check_database_connection
from app.services.intel.health import intel_health

logger = logging.getLogger("aggreyshield.health")
router = APIRouter(tags=["Health"])

_WARN_EVERY_SECONDS = 3600          # health checks run every few seconds; warn about stale lists hourly
# None = "never warned yet". (Do NOT start at 0.0: time.monotonic() counts from machine boot, so on a
# host that has been up for under an hour "now - 0" is below the limit and the first warning would be
# suppressed for up to an hour.)
_last_intel_warning: float | None = None


def _clock() -> float:
    return time.monotonic()


async def _threat_intel_summary() -> dict:
    global _last_intel_warning
    try:
        async with AsyncSessionLocal() as db:
            summary = await intel_health(db)
    except Exception:  # noqa: BLE001 - health must never raise
        return {"status": "unavailable", "sources_loaded": 0, "sources_expected": 0,
                "oldest_refresh_age_hours": None}
    now = _clock()
    due = _last_intel_warning is None or now - _last_intel_warning >= _WARN_EVERY_SECONDS
    if summary["status"] in ("not_loaded", "stale") and due:
        _last_intel_warning = now
        logger.warning(
            "threat lists are %s (oldest refresh: %s h). Known-bad lookups are weakened. "
            "Run: python -m app.services.intel.cli refresh --all --if-stale",
            summary["status"], summary["oldest_refresh_age_hours"],
        )
    return summary


@router.get("/health", summary="Readiness check (API + database + threat-list freshness)")
async def readiness_check(response: Response):
    db_connected = await check_database_connection()

    if not db_connected:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE

    threat_intel = (
        await _threat_intel_summary()
        if db_connected
        else {"status": "unavailable", "sources_loaded": 0, "sources_expected": 0, "oldest_refresh_age_hours": None}
    )

    return {
        "status": "ok" if db_connected else "degraded",
        "app_name": settings.APP_NAME,
        "app_version": settings.APP_VERSION,
        "environment": settings.ENVIRONMENT,
        "database": "connected" if db_connected else "unavailable",
        "threat_intel": threat_intel,
    }
