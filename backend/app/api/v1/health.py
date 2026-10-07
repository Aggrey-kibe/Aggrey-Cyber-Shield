"""
Versioned readiness endpoint.

GET /api/v1/health checks both the API process and live database
connectivity, and returns HTTP 503 if the database is unreachable so that
orchestrators (ECS/ALB target groups) stop routing traffic to this instance.

This is distinct from the plain GET /health liveness endpoint defined in
main.py, which has zero dependencies and is used for fast container restarts
decisions rather than traffic routing decisions.
"""
from fastapi import APIRouter, Response, status

from app.core.config import settings
from app.db.session import check_database_connection

router = APIRouter(tags=["Health"])


@router.get("/health", summary="Readiness check (API + database)")
async def readiness_check(response: Response):
    db_connected = await check_database_connection()

    if not db_connected:
        response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE

    return {
        "status": "ok" if db_connected else "degraded",
        "app_name": settings.APP_NAME,
        "app_version": settings.APP_VERSION,
        "environment": settings.ENVIRONMENT,
        "database": "connected" if db_connected else "unavailable",
    }
