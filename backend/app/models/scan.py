"""Scan history: one row per link/message a user asked Aggrey Shield to check."""
import uuid
from datetime import datetime, timezone

from sqlalchemy import JSON, DateTime, ForeignKey, Index, Integer, String, Uuid
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class Scan(Base):
    """
    A completed scan.

    Privacy: `input_preview` never holds a full message or a URL query string
    (see scan_service). `input_sha256` lets the owner recognise a repeat scan
    without keeping the raw input.
    """

    __tablename__ = "scans"
    __table_args__ = (Index("ix_scans_user_created", "user_id", "created_at"),)

    id: Mapped[uuid.UUID] = mapped_column(Uuid, primary_key=True, default=uuid.uuid4)

    user_id: Mapped[uuid.UUID] = mapped_column(
        Uuid,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )

    scan_type: Mapped[str] = mapped_column(String(16), nullable=False)  # url | message
    input_preview: Mapped[str] = mapped_column(String(300), nullable=False)
    input_sha256: Mapped[str] = mapped_column(String(64), nullable=False)
    risk_score: Mapped[int] = mapped_column(Integer, nullable=False)
    verdict: Mapped[str] = mapped_column(String(32), nullable=False)

    # Findings and explanations, stored so history never needs re-analysis.
    result: Mapped[dict] = mapped_column(
        JSON().with_variant(JSONB(), "postgresql"), nullable=False
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )
