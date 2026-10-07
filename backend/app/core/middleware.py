"""
Security and observability middleware.

- SecurityHeadersMiddleware: adds defensive HTTP response headers to every
  response, including error responses.
- RequestContextLoggingMiddleware: tags every request with a unique
  request ID (returned in X-Request-ID) and logs method/path/status/duration.
  The request ID is also available to route handlers via request.state.request_id,
  which later phases (audit logging in particular) will reuse.
"""
import logging
import time
import uuid

from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response

from app.core.config import settings

request_logger = logging.getLogger("aggreyshield.request")


class RequestContextLoggingMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id

        start_time = time.perf_counter()
        response: Response = await call_next(request)
        duration_ms = (time.perf_counter() - start_time) * 1000

        response.headers["X-Request-ID"] = request_id

        request_logger.info(
            "%s %s -> %s (%.2fms) request_id=%s",
            request.method,
            request.url.path,
            response.status_code,
            duration_ms,
            request_id,
        )

        return response


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        response: Response = await call_next(request)

        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Permissions-Policy"] = "geolocation=(), microphone=(), camera=()"
        response.headers["Cache-Control"] = "no-store"

        # This API only ever returns JSON, so a strict CSP mainly guards
        # against a misconfigured proxy ever serving an HTML error page
        # that could be used for injection.
        response.headers["Content-Security-Policy"] = "default-src 'none'; frame-ancestors 'none'"

        # HSTS only makes sense once traffic is actually served over HTTPS,
        # which in production happens at the AWS load balancer (Phase 13).
        if settings.is_production:
            response.headers["Strict-Transport-Security"] = "max-age=63072000; includeSubDomains"

        return response
