"""
Shared pytest fixtures.

No live PostgreSQL connection is required to run the Phase 1 test suite —
the readiness endpoint is designed to degrade gracefully (HTTP 503) rather
than raise when the database is unreachable, so tests assert on that
behavior rather than requiring Docker/Postgres to be running.
"""
import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="session")
def client() -> TestClient:
    return TestClient(app)


# ---------------------------------------------------------------------------
# Database-backed fixtures for end-to-end auth tests.
#
# SAFETY: these tests create and DELETE users. They refuse to run unless the
# configured database name ends with "_test", so they can never touch a
# development or production database by accident.
# ---------------------------------------------------------------------------
import httpx
import pytest_asyncio
from sqlalchemy import text
from sqlalchemy.engine import make_url

from app.core.config import settings
from app.db.session import AsyncSessionLocal, engine


def _is_test_database() -> bool:
    name = make_url(settings.DATABASE_URL).database or ""
    return name.endswith("_test")


@pytest_asyncio.fixture
async def clean_db():
    """Empty all auth tables before each test (test database only)."""
    if not _is_test_database():
        pytest.skip(
            "DB tests only run against a database whose name ends with "
            "'_test'. Set DATABASE_URL to a dedicated test database."
        )
    # Connections pooled by earlier tests belong to other (closed) event
    # loops; discard them without trying to close them.
    await engine.dispose(close=False)
    async with AsyncSessionLocal() as session:
        await session.execute(text("TRUNCATE users CASCADE"))
        await session.commit()
    yield
    # Each test has its own event loop; drop pooled connections between tests.
    await engine.dispose()


@pytest_asyncio.fixture
async def api(clean_db):
    transport = httpx.ASGITransport(app=app)
    async with httpx.AsyncClient(
        transport=transport, base_url="http://test"
    ) as client:
        yield client


@pytest.fixture
def sent_emails(monkeypatch):
    """Capture outgoing verification emails instead of calling Resend."""
    outbox: list[tuple[str, str]] = []

    def fake_send(recipient_email: str, verification_code: str) -> None:
        outbox.append((recipient_email, verification_code))

    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email", fake_send
    )
    return outbox
