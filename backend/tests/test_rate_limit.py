"""Tests for rate limiting: the limiter itself, then real API behaviour."""
import pytest

from app.core import rate_limit
from app.core.config import settings
from app.core.rate_limit import SlidingWindowLimiter, limiter

PASSWORD = "Str0ng-Passw0rd-Test"


# ---------------------------------------------------------------- unit tests
class FakeClock:
    def __init__(self):
        self.now = 1000.0

    def __call__(self):
        return self.now


def test_allows_up_to_limit_then_blocks_with_retry_after():
    clock = FakeClock()
    lim = SlidingWindowLimiter(clock=clock)
    for _ in range(3):
        assert lim.hit("k", 3, 60) == 0
    retry = lim.hit("k", 3, 60)
    assert retry == 60  # nothing has aged out yet


def test_window_slides_and_retry_after_counts_down():
    clock = FakeClock()
    lim = SlidingWindowLimiter(clock=clock)
    for _ in range(3):
        lim.hit("k", 3, 60)
    clock.now += 45
    assert lim.hit("k", 3, 60) == 15  # oldest hit expires in 15s
    clock.now += 15
    assert lim.hit("k", 3, 60) == 0  # allowed again


def test_blocked_attempts_are_not_counted():
    clock = FakeClock()
    lim = SlidingWindowLimiter(clock=clock)
    for _ in range(3):
        lim.hit("k", 3, 60)
    for _ in range(50):  # hammering while blocked must not extend the lockout
        assert lim.hit("k", 3, 60) > 0
    clock.now += 60
    assert lim.hit("k", 3, 60) == 0


def test_keys_are_independent_and_clear_works():
    lim = SlidingWindowLimiter(clock=FakeClock())
    for _ in range(2):
        lim.hit("a", 2, 60)
    assert lim.hit("a", 2, 60) > 0
    assert lim.hit("b", 2, 60) == 0
    lim.clear("a")
    assert lim.hit("a", 2, 60) == 0


def test_expired_keys_are_pruned():
    clock = FakeClock()
    lim = SlidingWindowLimiter(clock=clock)
    for i in range(100):
        lim.hit(f"k{i}", 5, 10)
    assert len(lim) == 100
    clock.now += 120  # all windows expired, prune interval passed
    lim.hit("fresh", 5, 10)
    assert len(lim) == 1


def test_memory_is_capped_under_unique_key_flood(monkeypatch):
    monkeypatch.setattr(SlidingWindowLimiter, "MAX_KEYS", 1000)
    lim = SlidingWindowLimiter(clock=FakeClock())
    for i in range(5000):
        lim.hit(f"attacker{i}", 5, 3600)
    assert len(lim) <= 1000


class _Req:
    def __init__(self, host, xff=None):
        self.client = type("C", (), {"host": host})()
        self.headers = {"x-forwarded-for": xff} if xff else {}


def test_forwarded_header_ignored_unless_proxy_trusted(monkeypatch):
    monkeypatch.setattr(settings, "TRUST_PROXY_HEADERS", False)
    assert rate_limit.client_ip(_Req("1.1.1.1", "9.9.9.9")) == "1.1.1.1"


def test_forwarded_header_last_entry_used_when_trusted(monkeypatch):
    monkeypatch.setattr(settings, "TRUST_PROXY_HEADERS", True)
    # a client-forged first entry must not be believed; the proxy-appended
    # last entry is
    req = _Req("10.0.0.1", "6.6.6.6, 203.0.113.7")
    assert rate_limit.client_ip(req) == "203.0.113.7"


# ------------------------------------------------------- API (real database)
async def _login(api, email, password="Wrong-Passw0rd-1"):
    return await api.post(
        "/api/v1/auth/login", json={"email": email, "password": password}
    )


@pytest.mark.asyncio
async def test_login_blocked_after_10_failures_for_one_email(api):
    for _ in range(10):
        assert (await _login(api, "victim@example.com")).status_code == 401
    blocked = await _login(api, "victim@example.com")
    assert blocked.status_code == 429
    assert int(blocked.headers["Retry-After"]) > 0
    body = blocked.json()["error"]
    assert body["code"] == "TOO_MANY_REQUESTS"
    assert "try again" in body["message"].lower()

    # a different email from the same address is NOT affected
    assert (await _login(api, "someone.else@example.com")).status_code == 401


@pytest.mark.asyncio
async def test_login_lockout_ends_when_window_passes(api, monkeypatch):
    for _ in range(10):
        await _login(api, "victim@example.com")
    assert (await _login(api, "victim@example.com")).status_code == 429

    real_clock = limiter.clock
    monkeypatch.setattr(limiter, "clock", lambda: real_clock() + 16 * 60)
    assert (await _login(api, "victim@example.com")).status_code == 401


@pytest.mark.asyncio
async def test_successful_login_resets_failed_counter(api, sent_emails):
    email = "member@example.com"
    await api.post(
        "/api/v1/auth/register",
        json={"email": email, "full_name": "Member One", "password": PASSWORD},
    )
    await api.post(
        "/api/v1/auth/verify-email",
        json={"email": email, "code": sent_emails[0][1]},
    )
    for _ in range(9):
        assert (await _login(api, email)).status_code == 401
    good = await _login(api, email, PASSWORD)
    assert good.status_code == 200
    # counter was cleared: 9 more failures are still allowed
    for _ in range(9):
        assert (await _login(api, email)).status_code == 401


@pytest.mark.asyncio
async def test_register_same_email_limited_to_5_per_hour(api, sent_emails):
    payload = {
        "email": "bomb.target@example.com",
        "full_name": "Target",
        "password": PASSWORD,
    }
    codes = []
    codes.append((await api.post("/api/v1/auth/register", json=payload)).status_code)
    for _ in range(4):
        codes.append(
            (await api.post("/api/v1/auth/register", json=payload)).status_code
        )
    assert codes == [201, 409, 409, 409, 409]
    assert (
        await api.post("/api/v1/auth/register", json=payload)
    ).status_code == 429
    assert len(sent_emails) == 1  # the victim inbox received exactly one email


@pytest.mark.asyncio
async def test_resend_verification_limited_per_email(api, sent_emails):
    for _ in range(5):
        r = await api.post(
            "/api/v1/auth/resend-verification",
            json={"email": "nobody@example.com"},
        )
        assert r.status_code == 400  # unknown / cooldown, but counted
    r = await api.post(
        "/api/v1/auth/resend-verification", json={"email": "nobody@example.com"}
    )
    assert r.status_code == 429


@pytest.mark.asyncio
async def test_forgot_password_limit_identical_for_known_and_unknown(
    api, sent_emails
):
    await api.post(
        "/api/v1/auth/register",
        json={
            "email": "known@example.com",
            "full_name": "Known User",
            "password": PASSWORD,
        },
    )
    results = {}
    for email in ("known@example.com", "unknown@example.com"):
        statuses = []
        for _ in range(6):
            r = await api.post(
                "/api/v1/auth/forgot-password", json={"email": email}
            )
            statuses.append(r.status_code)
        results[email] = statuses
    # Same behaviour whether or not the account exists: 5 allowed, 6th limited
    assert results["known@example.com"] == results["unknown@example.com"]
    assert results["known@example.com"] == [200, 200, 200, 200, 200, 429]


@pytest.mark.asyncio
async def test_verify_email_limited_per_email(api, sent_emails):
    for _ in range(10):
        r = await api.post(
            "/api/v1/auth/verify-email",
            json={"email": "x@example.com", "code": "123456"},
        )
        assert r.status_code == 400
    r = await api.post(
        "/api/v1/auth/verify-email",
        json={"email": "x@example.com", "code": "123456"},
    )
    assert r.status_code == 429


@pytest.mark.asyncio
async def test_ip_limit_on_registration_across_many_emails(api, sent_emails):
    for i in range(20):
        r = await api.post(
            "/api/v1/auth/register",
            json={
                "email": f"user{i}@example.com",
                "full_name": "Bulk User",
                "password": PASSWORD,
            },
        )
        assert r.status_code == 201
    r = await api.post(
        "/api/v1/auth/register",
        json={
            "email": "user21@example.com",
            "full_name": "Bulk User",
            "password": PASSWORD,
        },
    )
    assert r.status_code == 429


@pytest.mark.asyncio
async def test_forged_forwarded_header_cannot_bypass_ip_limit(api, sent_emails):
    # TRUST_PROXY_HEADERS is False by default: a different forged address on
    # every request must not reset the per-IP counter.
    assert settings.TRUST_PROXY_HEADERS is False
    statuses = []
    for i in range(21):
        r = await api.post(
            "/api/v1/auth/register",
            headers={"X-Forwarded-For": f"198.51.100.{i}"},
            json={
                "email": f"forger{i}@example.com",
                "full_name": "Forger",
                "password": PASSWORD,
            },
        )
        statuses.append(r.status_code)
    assert statuses[:20] == [201] * 20
    assert statuses[20] == 429


@pytest.mark.asyncio
async def test_unauthorized_responses_now_carry_www_authenticate(api):
    r = await _login(api, "ghost@example.com")
    assert r.status_code == 401
    assert r.headers.get("www-authenticate") == "Bearer"
