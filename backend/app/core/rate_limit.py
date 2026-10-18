"""
In-process sliding-window rate limiting for sensitive endpoints.

Why: login, registration, verification, resend and password-recovery
endpoints are brute-force and email-bombing targets. Each registration or
recovery request also sends a real email, and the email provider has a daily
quota, so abuse here can stop genuine users from receiving their codes.

Design notes (read before changing the numbers):
- Two kinds of limit are combined. Per-EMAIL limits are the precise ones
  (they protect one account / one inbox). Per-IP limits are deliberately
  GENEROUS: many mobile users share one public IP behind carrier NAT, so a
  strict per-IP limit would block innocent people.
- Failed-login limits per email can be used to lock a victim out for a
  while by an attacker who knows their email. That trade-off is accepted
  (the lockout is short and a successful login clears the counter).
- State lives in this process's memory. That is correct for ONE server
  process (the current Docker CMD). If you later run several workers or
  containers, each keeps its own counters; move the store to Redis then.
- Behind a reverse proxy / load balancer every request appears to come from
  the proxy. Set TRUST_PROXY_HEADERS=true ONLY when exactly one trusted
  proxy (e.g. an AWS ALB) sits in front, because then the last entry of
  X-Forwarded-For is the real client address. Never enable it otherwise:
  clients could forge the header and dodge the IP limits.
"""
import itertools
import logging
import math
import time
from collections import deque
from dataclasses import dataclass
from typing import Awaitable, Callable

from fastapi import HTTPException, Request, status

from app.core.config import settings

logger = logging.getLogger("aggreyshield.ratelimit")


@dataclass(frozen=True)
class Rule:
    """At most `limit` hits per `window_seconds` for one key."""

    name: str
    limit: int
    window_seconds: int


# --- Policy -----------------------------------------------------------------
# Per-IP rules are generous on purpose (shared mobile IPs). Per-email rules
# are the strict ones.
LOGIN_IP = Rule("login_ip", 60, 15 * 60)
LOGIN_EMAIL = Rule("login_email", 10, 15 * 60)  # cleared by a successful login
REGISTER_IP = Rule("register_ip", 20, 60 * 60)
REGISTER_EMAIL = Rule("register_email", 5, 60 * 60)
VERIFY_IP = Rule("verify_ip", 60, 15 * 60)
VERIFY_EMAIL = Rule("verify_email", 10, 15 * 60)
RESEND_IP = Rule("resend_ip", 20, 60 * 60)
RESEND_EMAIL = Rule("resend_email", 5, 60 * 60)
FORGOT_IP = Rule("forgot_ip", 20, 60 * 60)
FORGOT_EMAIL = Rule("forgot_email", 5, 60 * 60)
RESET_IP = Rule("reset_ip", 30, 60 * 60)
CHANGE_PASSWORD_IP = Rule("change_password_ip", 20, 15 * 60)


class SlidingWindowLimiter:
    """Sliding-window counter keyed by an arbitrary string."""

    MAX_KEYS = 100_000
    PRUNE_INTERVAL_SECONDS = 60.0

    def __init__(self, clock: Callable[[], float] = time.monotonic) -> None:
        self.clock = clock  # replaceable in tests
        self._hits: dict[str, deque[float]] = {}
        self._windows: dict[str, int] = {}
        self._last_prune = self.clock()

    def hit(self, key: str, limit: int, window_seconds: int) -> int:
        """
        Record one attempt.

        Returns 0 when the attempt is allowed, otherwise the number of whole
        seconds until the caller may try again. Blocked attempts are NOT
        recorded, so a lockout ends when the oldest counted attempt expires.
        """
        now = self.clock()
        self._maybe_prune(now)

        bucket = self._hits.get(key)
        if bucket is None:
            bucket = deque()
            self._hits[key] = bucket
        self._windows[key] = window_seconds

        cutoff = now - window_seconds
        while bucket and bucket[0] <= cutoff:
            bucket.popleft()

        if len(bucket) >= limit:
            return max(1, math.ceil(bucket[0] + window_seconds - now))

        bucket.append(now)
        return 0

    def clear(self, key: str) -> None:
        self._hits.pop(key, None)
        self._windows.pop(key, None)

    def reset(self) -> None:
        self._hits.clear()
        self._windows.clear()
        self._last_prune = self.clock()

    def __len__(self) -> int:
        return len(self._hits)

    def _maybe_prune(self, now: float) -> None:
        if (
            now - self._last_prune < self.PRUNE_INTERVAL_SECONDS
            and len(self._hits) <= self.MAX_KEYS
        ):
            return
        self._last_prune = now

        for key in list(self._hits):
            bucket = self._hits[key]
            cutoff = now - self._windows.get(key, 0)
            while bucket and bucket[0] <= cutoff:
                bucket.popleft()
            if not bucket:
                del self._hits[key]
                self._windows.pop(key, None)

        # Memory safety: if an attacker floods with unique keys, evict the
        # oldest 10% rather than grow without bound.
        if len(self._hits) > self.MAX_KEYS:
            for key in list(itertools.islice(self._hits, self.MAX_KEYS // 10)):
                del self._hits[key]
                self._windows.pop(key, None)


limiter = SlidingWindowLimiter()


def client_ip(request: Request) -> str:
    """Best-effort client address (see TRUST_PROXY_HEADERS notes above)."""
    if settings.TRUST_PROXY_HEADERS:
        forwarded = request.headers.get("x-forwarded-for", "")
        last = forwarded.split(",")[-1].strip()
        if last:
            return last
    return request.client.host if request.client else "unknown"


def _enforce(key: str, rule: Rule, scope: str) -> None:
    retry_after = limiter.hit(key, rule.limit, rule.window_seconds)
    if retry_after:
        # Never log emails; the rule name and scope are enough to investigate.
        logger.warning(
            "rate_limited rule=%s scope=%s retry_after=%ss",
            rule.name,
            scope,
            retry_after,
        )
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail=f"Too many requests. Please try again in {retry_after} seconds.",
            headers={"Retry-After": str(retry_after)},
        )


def limit_by_ip(rule: Rule) -> Callable[[Request], Awaitable[None]]:
    """FastAPI dependency: apply `rule` to the caller's IP address."""

    async def dependency(request: Request) -> None:
        ip = client_ip(request)
        _enforce(f"{rule.name}:{ip}", rule, "ip")

    return dependency


def enforce_email(rule: Rule, email: str) -> None:
    """Apply `rule` to an email address (call after the body is validated)."""
    _enforce(f"{rule.name}:{email.strip().lower()}", rule, "email")


def clear_email(rule: Rule, email: str) -> None:
    """Forget the attempts for an email (used after a successful login)."""
    limiter.clear(f"{rule.name}:{email.strip().lower()}")
