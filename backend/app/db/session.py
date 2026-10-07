"""
Database session configuration (PostgreSQL via SQLAlchemy async engine).

Phase 1 scope: prepare a working, poolable connection to PostgreSQL and a
non-fatal connectivity check for the readiness endpoint.

Explicitly OUT of scope for Phase 1 (arrives in Phase 3):
- ORM models / declarative Base
- Alembic migrations
- Any actual tables

Because no tables exist yet, get_db() is provided now so route handlers in
later phases can depend on it immediately, but nothing in Phase 1 queries
real data.
"""
import logging

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import settings

logger = logging.getLogger("aggreyshield.db")

engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    pool_pre_ping=True,
    pool_size=5,
    max_overflow=10,
    future=True,
)

AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)


async def get_db():
    """
    FastAPI dependency that yields a database session and guarantees it is
    closed afterwards, even if the request raises.
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


async def check_database_connection() -> bool:
    """
    Lightweight, non-raising connectivity check used by the readiness
    endpoint. Returns False instead of raising so a missing/unreachable
    database degrades the readiness response rather than crashing the app —
    important in Phase 1, where PostgreSQL may not be running yet.
    """
    try:
        async with engine.connect() as connection:
            await connection.execute(text("SELECT 1"))
        return True
    except Exception:
        logger.warning("database_connectivity_check_failed", exc_info=True)
        return False
