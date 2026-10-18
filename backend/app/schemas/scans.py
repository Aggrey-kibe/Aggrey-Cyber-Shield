"""Request and response schemas for scanning and scan history."""
import uuid
from datetime import datetime

from pydantic import BaseModel, Field


class UrlScanRequest(BaseModel):
    url: str = Field(min_length=1, max_length=2048)


class MessageScanRequest(BaseModel):
    message: str = Field(min_length=1, max_length=5000)


class FindingOut(BaseModel):
    id: str
    severity: str
    title: str
    explanation: str
    evidence: str | None = None


class UrlResultOut(BaseModel):
    url: str
    host: str | None
    registered_domain: str | None
    risk_score: int
    verdict: str
    verdict_label: str
    findings: list[FindingOut]


class ScanResponse(BaseModel):
    id: uuid.UUID
    scan_type: str
    risk_score: int = Field(ge=0, le=100)
    verdict: str
    verdict_label: str
    summary: str
    recommendation: str
    confidence: str
    findings: list[FindingOut]
    urls: list[UrlResultOut] = []
    limitations: list[str]
    input_preview: str
    created_at: datetime


class ScanListItem(BaseModel):
    id: uuid.UUID
    scan_type: str
    risk_score: int
    verdict: str
    verdict_label: str
    input_preview: str
    created_at: datetime


class ScanListResponse(BaseModel):
    items: list[ScanListItem]
    total: int
    limit: int
    offset: int
