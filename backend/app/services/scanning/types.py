"""Shared result types and scoring for the scanning engine."""
from dataclasses import dataclass, field

SEVERITIES = ("info", "low", "medium", "high")

LIMITATIONS = (
    "This check looks only at the address or text you gave it. It does not "
    "open the link, inspect the page, or consult live threat databases yet. "
    "In our own tests on real phishing links, rules like these missed most "
    "of them, so a clean result means very little.",
    "New scams can look clean. If you are unsure, contact the organisation "
    "through its official app, website or phone number, not through this "
    "message or link.",
)


@dataclass(frozen=True)
class Finding:
    """One reason that raised (or explains) the risk score."""

    id: str
    severity: str  # one of SEVERITIES
    title: str
    explanation: str
    weight: int
    evidence: str | None = None

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "severity": self.severity,
            "title": self.title,
            "explanation": self.explanation,
            "evidence": self.evidence,
        }


# (minimum score, verdict id, label, summary, recommendation)
_VERDICTS = (
    (
        50,
        "likely_dangerous",
        "Likely dangerous",
        "This shows several strong signs of a scam or phishing attempt.",
        "Do not open the link, reply, install anything, or send money or "
        "codes. If it claims to be from a company, contact that company "
        "directly using its official app or number.",
    ),
    (
        25,
        "suspicious",
        "Suspicious",
        "This has signs that scammers commonly use.",
        "Be careful. Do not enter passwords, PINs or codes, and do not send "
        "money. Check with the organisation through an official channel first.",
    ),
    (
        1,
        "low_risk",
        "Low risk",
        "Minor warning signs were found, but nothing strongly suspicious.",
        "Stay alert. If anything asks for passwords, PINs, codes or money, "
        "stop and verify it independently.",
    ),
    (
        0,
        "no_warning_signs",
        "No warning signs found",
        "Nothing suspicious was found in the address or text. This is NOT a "
        "guarantee that it is safe.",
        "Even so, never share passwords, PINs or one-time codes, and verify "
        "unexpected requests through an official channel.",
    ),
)


def verdict_for(score: int) -> tuple[str, str, str, str]:
    """Return (verdict_id, label, summary, recommendation) for a 0-100 score."""
    for minimum, verdict_id, label, summary, recommendation in _VERDICTS:
        if score >= minimum:
            return verdict_id, label, summary, recommendation
    raise AssertionError("unreachable")


def score_from(findings: list[Finding]) -> int:
    return max(0, min(100, sum(f.weight for f in findings)))


@dataclass
class UrlAnalysis:
    input: str
    normalized_url: str
    host: str | None
    registered_domain: str | None
    score: int
    findings: list[Finding] = field(default_factory=list)

    @property
    def verdict(self) -> str:
        return verdict_for(self.score)[0]

    def to_dict(self) -> dict:
        verdict_id, label, summary, recommendation = verdict_for(self.score)
        return {
            "url": self.normalized_url,
            "host": self.host,
            "registered_domain": self.registered_domain,
            "risk_score": self.score,
            "verdict": verdict_id,
            "verdict_label": label,
            "findings": [f.to_dict() for f in self.findings],
        }
