"""SQLAlchemy declarative base for Aggrey Shield database models."""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Base class inherited by every SQLAlchemy ORM model."""
