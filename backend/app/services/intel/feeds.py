"""Feed definitions (what we trust, how long, and what a sane feed looks like)."""
from dataclasses import dataclass

from app.services.scanning.brands import BRANDS, FREE_HOSTING, URL_SHORTENERS

# Registered domains no feed may ever mark malicious as a whole: brand-owned
# domains cannot be attacker-owned, and whole shorteners/free-hosting roots
# would flag millions of innocent pages. (Single URLs on them may still match.)
#
# Added after auditing the real Phishing.Database domain feed against a list of the
# 10,000 most popular domains (2026-10-18): it listed 10 of them as whole sites.
# Review status: judged from general knowledge, NOT yet confirmed from primary sources;
# a maintainer should confirm. Re-run `python -m evaluation.intel_fp_audit` after every refresh.
#
# Legitimate services / CDNs: no domain-level listing under these is accepted.
REVIEWED_LEGITIMATE = frozenset({
    "gravatar.com",          # Automattic avatar service
    "nflxvideo.net",         # Netflix video delivery network
    "getpocket.com",         # Mozilla Pocket
    "mercadolibre.com.ar",   # MercadoLibre Argentina marketplace
})
# User-content platforms: individual pages/tenants get abused, but the platform root is legitimate.
# Only ROOT-level listings are refused; a listed tenant subdomain or exact URL still matches.
REVIEWED_PLATFORMS = frozenset({
    "pastebin.com", "angelfire.com", "unbouncepages.com", "abload.de", "yotpo.com",
})

BRAND_DOMAINS = frozenset(d for b in BRANDS for d in b.official) | REVIEWED_LEGITIMATE
SHARED_ROOTS = frozenset(FREE_HOSTING) | frozenset(URL_SHORTENERS) | REVIEWED_PLATFORMS


@dataclass(frozen=True)
class Feed:
    name: str
    url: str
    indicator_type: str      # "url" | "domain"
    category: str
    confidence: str          # how much weight a listing deserves: "medium" = aggregated, status-checked
    license: str
    attribution: str
    ttl_days: int            # safety net: entries expire this long after last being seen
    max_bytes: int
    min_accepted: int        # a healthy full feed has at least this many valid entries
    max_age_hours: int       # older than this = "stale"
    refresh_after_hours: int = 20   # `refresh --if-stale` only reloads a feed older than this


_BASE = "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/"

FEEDS: dict[str, Feed] = {
    "phishing_database_links": Feed(
        name="phishing_database_links",
        url=_BASE + "phishing-links-ACTIVE.txt",
        indicator_type="url",
        category="phishing",
        confidence="medium",
        license="MIT",
        attribution="Phishing.Database (github.com/Phishing-Database/Phishing.Database)",
        ttl_days=14,
        max_bytes=150 * 1024 * 1024,
        min_accepted=50_000,
        max_age_hours=72,
    ),
    "phishing_database_domains": Feed(
        name="phishing_database_domains",
        url=_BASE + "phishing-domains-ACTIVE.txt",
        indicator_type="domain",
        category="phishing",
        confidence="medium",
        license="MIT",
        attribution="Phishing.Database (github.com/Phishing-Database/Phishing.Database)",
        ttl_days=14,
        max_bytes=60 * 1024 * 1024,
        min_accepted=20_000,
        max_age_hours=72,
    ),
}
