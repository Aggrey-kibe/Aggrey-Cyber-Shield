"""Threat-intelligence storage: indicators from external feeds and a log of every refresh."""
import uuid
from datetime import datetime, timezone

from sqlalchemy import (
    JSON, BigInteger, DateTime, ForeignKey, Integer, LargeBinary, String, UniqueConstraint, Uuid,
)
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


def _now() -> datetime:
    return datetime.now(timezone.utc)


class ThreatFeedRun(Base):
    """One attempt to refresh a feed. Kept forever as an audit trail."""

    __tablename__ = "threat_feed_runs"

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)
    source: Mapped[str] = mapped_column(String(48), nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(12), nullable=False)  # running|ok|rejected|failed
    partial: Mapped[bool] = mapped_column(nullable=False, default=False)
    started_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=_now, nullable=False)
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)
    lines_total: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    accepted: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    rejected: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    removed: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    bytes_downloaded: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    content_sha256: Mapped[str | None] = mapped_column(String(64), nullable=True)
    error: Mapped[str | None] = mapped_column(String(500), nullable=True)
    details: Mapped[dict] = mapped_column(
        JSON().with_variant(JSONB(), "postgresql"), nullable=False, default=dict
    )


class ThreatIndicator(Base):
    """
    One known-bad indicator.

    Only a SHA-256 of the canonical value is stored (plus the registered
    domain for reporting), never the hostile URL text: nothing here can be
    clicked, fetched or executed by accident.
    """

    __tablename__ = "threat_indicators"
    __table_args__ = (
        # Leads with (type, hash) so it also serves lookups: one index, not two.
        UniqueConstraint("indicator_type", "value_sha256", "source", name="uq_indicator_identity"),
    )

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    source: Mapped[str] = mapped_column(String(48), nullable=False)
    indicator_type: Mapped[str] = mapped_column(String(8), nullable=False)  # url | domain
    value_sha256: Mapped[bytes] = mapped_column(LargeBinary(32), nullable=False)  # raw SHA-256
    registered_domain: Mapped[str | None] = mapped_column(String(255), nullable=True)
    category: Mapped[str] = mapped_column(String(24), nullable=False)
    first_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    feed_run_id: Mapped[uuid.UUID] = mapped_column(
        Uuid, ForeignKey("threat_feed_runs.id", ondelete="CASCADE"), nullable=False
    )
