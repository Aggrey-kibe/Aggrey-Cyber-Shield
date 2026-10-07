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
