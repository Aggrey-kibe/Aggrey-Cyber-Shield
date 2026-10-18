"""
Aggrey Shield backend — application entrypoint.

Run locally with:
    uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.auth import router as auth_router_v1
from app.api.v1.health import router as health_router_v1
from app.api.v1.scans import router as scans_router_v1
from app.api.v1.users import router as users_router_v1
from app.core.config import settings
from app.core.exceptions import register_exception_handlers
from app.core.logging_config import configure_logging
from app.core.middleware import RequestContextLoggingMiddleware, SecurityHeadersMiddleware

configure_logging()

app = FastAPI(
    title=settings.APP_NAME,
    description=(
        "Aggrey Shield — cybersecurity, fraud-detection, and digital-safety "
        "platform API. Developed by Aggrey Kibe Kwamboka (Kenya)."
    ),
    version=settings.APP_VERSION,
    docs_url="/api/v1/docs" if not settings.is_production else None,
    redoc_url="/api/v1/redoc" if not settings.is_production else None,
    openapi_url="/api/v1/openapi.json" if not settings.is_production else None,
)

# --- Middleware ---
# Order matters: middleware added last wraps the response chain outermost,
# so it still sees (and can log/header-stamp) responses produced by the
# exception handlers registered below.
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RequestContextLoggingMiddleware)

# --- Exception handlers (consistent error envelope, no leaked internals) ---
register_exception_handlers(app)

# --- Routers ---
app.include_router(health_router_v1, prefix=settings.API_V1_STR)
app.include_router(auth_router_v1, prefix=settings.API_V1_STR)
app.include_router(users_router_v1, prefix=settings.API_V1_STR)
app.include_router(scans_router_v1, prefix=settings.API_V1_STR)

@app.get("/health", tags=["Health"], summary="Liveness check")
async def liveness_check():
    """
    Liveness probe: confirms the process is up and able to respond.
    Deliberately has zero dependencies (no DB, no cache) so an orchestrator
    can use it to decide whether to restart the container, independent of
    whether the database happens to be reachable right now.
    """
    return {"status": "ok", "app_name": settings.APP_NAME, "app_version": settings.APP_VERSION}
