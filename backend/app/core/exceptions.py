"""
Centralized exception handling.

Every error response — validation failures, HTTP errors, or unhandled
exceptions — is returned in the same shape:

    {
        "error": {
            "code": "SOME_CODE",
            "message": "Human readable message",
            "details": []
        }
    }

"details" is an optional list of field-level errors (used for validation
failures) and is an empty list when there is nothing more to report.

Unhandled exceptions are logged with full tracebacks server-side, but the
client only ever receives a generic message in production. This prevents
stack traces, file paths, or internal details from leaking to callers.
"""
import logging

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.config import settings

logger = logging.getLogger("aggreyshield.errors")

_STATUS_CODE_TO_ERROR_CODE = {
    400: "BAD_REQUEST",
    401: "UNAUTHORIZED",
    403: "FORBIDDEN",
    404: "NOT_FOUND",
    405: "METHOD_NOT_ALLOWED",
    409: "CONFLICT",
    422: "VALIDATION_ERROR",
    429: "TOO_MANY_REQUESTS",
}


def _error_response(
    code: str,
    message: str,
    status_code: int,
    details=None,
    headers: dict[str, str] | None = None,
) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        headers=headers,
        content={
            "error": {
                "code": code,
                "message": message,
                "details": details or [],
            }
        },
    )


def _code_for_status(status_code: int) -> str:
    return _STATUS_CODE_TO_ERROR_CODE.get(status_code, "HTTP_ERROR")


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        details = [
            {
                "field": ".".join(str(part) for part in error["loc"]),
                "message": error["msg"],
            }
            for error in exc.errors()
        ]
        logger.warning("validation_error path=%s details=%s", request.url.path, details)
        return _error_response(
            code="VALIDATION_ERROR",
            message="Request validation failed.",
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            details=details,
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        logger.info(
            "http_exception path=%s status=%s detail=%s",
            request.url.path,
            exc.status_code,
            exc.detail,
        )
        return _error_response(
            code=_code_for_status(exc.status_code),
            message=str(exc.detail),
            status_code=exc.status_code,
            headers=exc.headers,
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        logger.exception("unhandled_exception path=%s", request.url.path)

        if settings.is_production:
            message = "An unexpected error occurred. Please try again later."
        else:
            message = f"{type(exc).__name__}: {exc}"

        return _error_response(
            code="INTERNAL_SERVER_ERROR",
            message=message,
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )
