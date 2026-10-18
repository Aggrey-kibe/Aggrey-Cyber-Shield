"""
Canonical forms for feed entries AND for the URLs users scan.

Ingestion and lookup MUST use the same functions, otherwise nothing matches.
The scheme is deliberately ignored (http vs https of the same address are the
same indicator) and userinfo is dropped, so 'https://google.com@evil.com/' is
looked up as evil.com, the place the browser really goes.
"""
import hashlib
import ipaddress
import re
from dataclasses import dataclass
from urllib.parse import urlsplit

import tldextract

MAX_URL_LENGTH = 2048
_EXTRACT = tldextract.TLDExtract(suffix_list_urls=(), cache_dir=None)
_NUMERIC_PART = re.compile(r"^(0x[0-9a-f]+|\d+)$", re.IGNORECASE)
_HOST_OK = re.compile(r"^[a-z0-9]([a-z0-9._-]{0,251}[a-z0-9])?$")
_BLOCKED_SUFFIXES = (".local", ".localhost", ".internal", ".lan", ".home", ".corp", ".test", ".invalid")


@dataclass(frozen=True)
class Canon:
    key: str                      # canonical text that is hashed (never stored)
    host: str
    registered_domain: str | None
    sha256: str


def _digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _clean_host(host: str) -> str | None:
    host = host.strip().strip("[]").rstrip(".").lower()
    if not host or len(host) > 253:
        return None
    try:
        host = host.encode("idna").decode("ascii")
    except UnicodeError:
        return None
    if host == "localhost" or host.endswith(_BLOCKED_SUFFIXES):
        return None
    try:
        return host if ipaddress.ip_address(host).is_global else None
    except ValueError:
        pass
    parts = host.split(".")
    if all(_NUMERIC_PART.match(p) for p in parts):
        return None  # obfuscated numeric address: ambiguous, refuse
    if not _HOST_OK.match(host) or any(not p or len(p) > 63 for p in parts):
        return None
    return host


def _registered(host: str) -> str | None:
    try:
        ipaddress.ip_address(host)
        return None
    except ValueError:
        return _EXTRACT(host).top_domain_under_public_suffix or None


def canonical_url(raw: str) -> Canon | None:
    """Canonical form of a URL, or None if it cannot be a valid public web address."""
    if not isinstance(raw, str):
        return None
    text = raw.strip()
    if not text or len(text) > MAX_URL_LENGTH or any(ord(c) < 32 or ord(c) == 127 for c in text):
        return None
    if "://" in text[:12]:
        if not text.lower().startswith(("http://", "https://")):
            return None
    else:
        text = "http://" + text.lstrip("/")
    try:
        parts = urlsplit(text)
        host = _clean_host(parts.hostname or "")
        port = parts.port
    except ValueError:
        return None
    if host is None:
        return None
    hostport = host if port in (None, 80, 443) else f"{host}:{port}"
    path = parts.path or "/"
    key = hostport + path + (f"?{parts.query}" if parts.query else "")
    return Canon(key=key, host=host, registered_domain=_registered(host), sha256=_digest("url:" + key))


def canonical_host(raw: str) -> Canon | None:
    """Canonical form of a bare host name (from a domain feed, or the host of a scanned URL)."""
    if not isinstance(raw, str) or "/" in raw or ":" in raw or "@" in raw:
        return None
    host = _clean_host(raw)
    if host is None:
        return None
    return Canon(key=host, host=host, registered_domain=_registered(host), sha256=_digest("host:" + host))
