"""
Structured logging configuration.

Development: human-readable single-line logs.
Production: JSON logs (one object per line) so they can be shipped to
CloudWatch/ELK/etc. and queried without regex parsing.

No request bodies, passwords, or tokens are ever logged by this module.
Individual endpoints are responsible for not passing sensitive values into
log messages.
"""
import json
import logging
import sys
from datetime import datetime, timezone

from app.core.config import settings


class JSONFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }

        request_id = getattr(record, "request_id", None)
        if request_id:
            payload["request_id"] = request_id

        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)

        return json.dumps(payload)


def configure_logging() -> None:
    handler = logging.StreamHandler(sys.stdout)

    if settings.is_production:
        handler.setFormatter(JSONFormatter())
    else:
        handler.setFormatter(
            logging.Formatter(
                fmt="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
                datefmt="%Y-%m-%d %H:%M:%S",
            )
        )

    root_logger = logging.getLogger()
    root_logger.setLevel(settings.LOG_LEVEL)
    root_logger.handlers = [handler]

    # Keep third-party access logs from drowning out application logs locally,
    # but let them through at INFO in production where they matter for audits.
    logging.getLogger("uvicorn.access").setLevel(
        logging.INFO if settings.is_production else logging.WARNING
    )
    logging.getLogger("uvicorn.error").setLevel(logging.INFO)
