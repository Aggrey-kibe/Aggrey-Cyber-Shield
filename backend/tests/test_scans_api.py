"""Scan endpoints and /users/me against a real database."""
import json

import pytest
from sqlalchemy import text

from app.db.session import AsyncSessionLocal

PASSWORD = "Str0ng-Passw0rd-Test"
DANGEROUS_URL = "http://safaricom.co.ke.verify-account.xyz/login"
BENIGN_URL = "https://www.google.com/search?q=cats"


async def signed_in(api, sent_emails, email, name="Test Member"):
    """Register, verify and sign in a real user; return auth headers + user id."""
    r = await api.post(
        "/api/v1/auth/register",
        json={"email": email, "full_name": name, "password": PASSWORD},
    )
    assert r.status_code == 201
    code = [c for to, c in sent_emails if to == email][-1]
    assert (
        await api.post("/api/v1/auth/verify-email", json={"email": email, "code": code})
    ).status_code == 200
    login = await api.post(
        "/api/v1/auth/login", json={"email": email, "password": PASSWORD}
    )
    assert login.status_code == 200
    return {"Authorization": f"Bearer {login.json()['access_token']}"}


async def _sql(statement, **params):
    async with AsyncSessionLocal() as session:
        result = await session.execute(text(statement), params)
        await session.commit()
        return result


@pytest.mark.asyncio
async def test_every_endpoint_requires_sign_in(api):
    some_id = "3f2b8c1e-8f6a-4c1e-9d3b-0a1b2c3d4e5f"
    calls = [
        ("GET", "/api/v1/users/me", None),
        ("POST", "/api/v1/scans/url", {"url": BENIGN_URL}),
        ("POST", "/api/v1/scans/message", {"message": "hello"}),
        ("GET", "/api/v1/scans", None),
        ("GET", f"/api/v1/scans/{some_id}", None),
        ("DELETE", f"/api/v1/scans/{some_id}", None),
    ]
    for method, path, body in calls:
        r = await api.request(method, path, json=body)
        assert r.status_code == 401, (method, path)


@pytest.mark.asyncio
async def test_url_scan_returns_explained_result_and_is_saved(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    r = await api.post("/api/v1/scans/url", json={"url": DANGEROUS_URL}, headers=h)
    assert r.status_code == 201
    body = r.json()
    assert body["scan_type"] == "url"
    assert body["verdict"] == "likely_dangerous"
    assert body["risk_score"] >= 50
    assert body["confidence"] == "limited"
    assert body["limitations"]
    assert body["recommendation"]
    ids = [f["id"] for f in body["findings"]]
    assert "brand_impersonation" in ids
    assert all(f["explanation"] for f in body["findings"])

    fetched = await api.get(f"/api/v1/scans/{body['id']}", headers=h)
    assert fetched.status_code == 200
    assert fetched.json()["risk_score"] == body["risk_score"]


@pytest.mark.asyncio
async def test_benign_url_is_not_called_safe(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    body = (await api.post("/api/v1/scans/url", json={"url": BENIGN_URL}, headers=h)).json()
    assert body["verdict"] == "no_warning_signs"
    assert "NOT a guarantee" in body["summary"]


@pytest.mark.asyncio
async def test_message_scan_includes_link_results(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    msg = "Your KRA account will be suspended. Verify now: http://kra-refund.top/claim"
    r = await api.post("/api/v1/scans/message", json={"message": msg}, headers=h)
    assert r.status_code == 201
    body = r.json()
    assert body["scan_type"] == "message"
    assert body["verdict"] == "likely_dangerous"
    assert body["urls"] and body["urls"][0]["registered_domain"] == "kra-refund.top"
    assert {"threat_account", "verify_prompt"} <= {f["id"] for f in body["findings"]}


@pytest.mark.asyncio
async def test_history_is_private_to_each_user(api, sent_emails):
    a = await signed_in(api, sent_emails, "a@example.com")
    b = await signed_in(api, sent_emails, "b@example.com")
    for _ in range(2):
        await api.post("/api/v1/scans/url", json={"url": DANGEROUS_URL}, headers=a)
    await api.post("/api/v1/scans/message", json={"message": "hi there"}, headers=a)
    await api.post("/api/v1/scans/url", json={"url": BENIGN_URL}, headers=b)

    la = (await api.get("/api/v1/scans", headers=a)).json()
    lb = (await api.get("/api/v1/scans", headers=b)).json()
    assert la["total"] == 3 and len(la["items"]) == 3
    assert lb["total"] == 1
    only_urls = (await api.get("/api/v1/scans?scan_type=url", headers=a)).json()
    assert only_urls["total"] == 2
    # newest first
    stamps = [i["created_at"] for i in la["items"]]
    assert stamps == sorted(stamps, reverse=True)


@pytest.mark.asyncio
async def test_other_users_scan_cannot_be_read_or_deleted(api, sent_emails):
    a = await signed_in(api, sent_emails, "a@example.com")
    b = await signed_in(api, sent_emails, "b@example.com")
    scan_id = (
        await api.post("/api/v1/scans/url", json={"url": DANGEROUS_URL}, headers=a)
    ).json()["id"]

    # B gets the same "not found" as for a scan that never existed
    assert (await api.get(f"/api/v1/scans/{scan_id}", headers=b)).status_code == 404
    assert (await api.delete(f"/api/v1/scans/{scan_id}", headers=b)).status_code == 404
    # ...and A's scan is untouched
    assert (await api.get(f"/api/v1/scans/{scan_id}", headers=a)).status_code == 200

    assert (await api.delete(f"/api/v1/scans/{scan_id}", headers=a)).status_code == 204
    assert (await api.get(f"/api/v1/scans/{scan_id}", headers=a)).status_code == 404


@pytest.mark.asyncio
async def test_pagination_and_bounds(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    for i in range(5):
        await api.post("/api/v1/scans/message", json={"message": f"hello {i}"}, headers=h)
    page = (await api.get("/api/v1/scans?limit=2&offset=1", headers=h)).json()
    assert page["total"] == 5 and len(page["items"]) == 2
    assert page["limit"] == 2 and page["offset"] == 1
    for bad in ("limit=0", "limit=101", "offset=-1", "scan_type=other"):
        assert (await api.get(f"/api/v1/scans?{bad}", headers=h)).status_code == 422


@pytest.mark.asyncio
async def test_stored_data_does_not_keep_secrets(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    url = "https://example.org/reset?token=SECRETTOKEN123&u=1#frag"
    r = await api.post("/api/v1/scans/url", json={"url": url}, headers=h)
    body = r.json()
    assert "SECRETTOKEN123" not in json.dumps(body)
    assert "?" not in body["input_preview"] and "#" not in body["input_preview"]

    msg = "Your code is 482913 and account 0012345678 will be locked"
    m = (await api.post("/api/v1/scans/message", json={"message": msg}, headers=h)).json()
    assert "482913" not in m["input_preview"]
    assert "0012345678" not in m["input_preview"]
    assert "•" in m["input_preview"]

    rows = (await _sql("SELECT input_preview, input_sha256, result::text AS r FROM scans")).all()
    assert rows
    for row in rows:
        assert len(row.input_sha256) == 64
        assert "SECRETTOKEN123" not in row.r and "482913" not in row.input_preview


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "bad_url",
    ["   ", "http://[::1", "http://", "x" * 2049, "http://exa\x00mple.com"],
)
async def test_bad_url_input_is_a_clean_422_not_a_500(api, sent_emails, bad_url):
    h = await signed_in(api, sent_emails, "a@example.com")
    r = await api.post("/api/v1/scans/url", json={"url": bad_url}, headers=h)
    assert r.status_code == 422
    assert r.json()["error"]["code"] == "VALIDATION_ERROR"


@pytest.mark.asyncio
async def test_bad_message_input_is_422(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    for bad in ("", "   ", "x" * 5001):
        r = await api.post("/api/v1/scans/message", json={"message": bad}, headers=h)
        assert r.status_code == 422


@pytest.mark.asyncio
async def test_users_me_returns_only_own_safe_profile(api, sent_emails):
    a = await signed_in(api, sent_emails, "a@example.com", "Alice A")
    b = await signed_in(api, sent_emails, "b@example.com", "Bob B")
    me = (await api.get("/api/v1/users/me", headers=a)).json()
    assert me["email"] == "a@example.com" and me["full_name"] == "Alice A"
    assert me["is_verified"] is True
    assert set(me) == {"id", "email", "full_name", "is_verified", "mfa_enabled", "created_at"}
    assert "password" not in json.dumps(me).lower()
    other = (await api.get("/api/v1/users/me", headers=b)).json()
    assert other["email"] == "b@example.com" and other["id"] != me["id"]


@pytest.mark.asyncio
async def test_forged_or_garbage_token_rejected(api):
    for token in ("garbage", "a.b.c", ""):
        r = await api.get("/api/v1/users/me", headers={"Authorization": f"Bearer {token}"})
        assert r.status_code == 401


@pytest.mark.asyncio
async def test_scan_rate_limit_per_user(api, sent_emails):
    a = await signed_in(api, sent_emails, "a@example.com")
    b = await signed_in(api, sent_emails, "b@example.com")
    for _ in range(30):
        assert (
            await api.post("/api/v1/scans/message", json={"message": "hi"}, headers=a)
        ).status_code == 201
    blocked = await api.post("/api/v1/scans/message", json={"message": "hi"}, headers=a)
    assert blocked.status_code == 429 and "retry-after" in blocked.headers
    # another user is unaffected
    assert (
        await api.post("/api/v1/scans/message", json={"message": "hi"}, headers=b)
    ).status_code == 201


@pytest.mark.asyncio
async def test_deleting_a_user_removes_their_scans(api, sent_emails):
    a = await signed_in(api, sent_emails, "a@example.com")
    await api.post("/api/v1/scans/url", json={"url": BENIGN_URL}, headers=a)
    assert (await _sql("SELECT count(*) FROM scans")).scalar_one() == 1
    await _sql("DELETE FROM users WHERE email = :e", e="a@example.com")
    assert (await _sql("SELECT count(*) FROM scans")).scalar_one() == 0
