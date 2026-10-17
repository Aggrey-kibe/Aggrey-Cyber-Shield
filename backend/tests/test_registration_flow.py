"""
End-to-end tests for new-member registration, email verification and login.

Real FastAPI app + real PostgreSQL. Only the outgoing email call is replaced
(so the test can read the code that would have been emailed); the code
generation, hashing, expiry, attempt limits, password hashing, token issue
and database logic are all the real implementations.
"""
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import text

from app.db.session import AsyncSessionLocal

EMAIL = "new.member@example.com"
PASSWORD = "Str0ng-Passw0rd-Test"
REGISTER = {"email": EMAIL, "full_name": "New Member", "password": PASSWORD}


async def _register(api, payload=None):
    return await api.post("/api/v1/auth/register", json=payload or REGISTER)


async def _login(api, password=PASSWORD, email=EMAIL):
    return await api.post(
        "/api/v1/auth/login", json={"email": email, "password": password}
    )


async def _verify(api, code, email=EMAIL):
    return await api.post(
        "/api/v1/auth/verify-email", json={"email": email, "code": code}
    )


async def _sql(statement: str, **params):
    async with AsyncSessionLocal() as session:
        result = await session.execute(text(statement), params)
        await session.commit()
        return result


@pytest.mark.asyncio
async def test_full_new_member_journey(api, sent_emails):
    # 1. Register
    response = await _register(api)
    assert response.status_code == 201
    assert len(sent_emails) == 1
    recipient, code = sent_emails[0]
    assert recipient == EMAIL
    assert len(code) == 6 and code.isdigit()

    # 2. Password and code are never stored in plain text
    row = (
        await _sql(
            "SELECT password_hash, is_verified FROM users WHERE email=:e",
            e=EMAIL,
        )
    ).one()
    assert row.password_hash.startswith("$argon2id$")
    assert PASSWORD not in row.password_hash
    assert row.is_verified is False
    stored = (
        await _sql("SELECT code_hash FROM email_verification_codes")
    ).scalar_one()
    assert stored != code and len(stored) == 64

    # 3. Cannot sign in before verifying
    assert (await _login(api)).status_code == 401

    # 4. Verify with the emailed code
    assert (await _verify(api, code)).status_code == 200

    # 5. Now sign in, and receive working tokens
    login = await _login(api)
    assert login.status_code == 200
    tokens = login.json()
    assert tokens["token_type"] == "bearer"
    assert tokens["access_token"] and tokens["refresh_token"]

    # 6. Refresh works once; reusing the old refresh token is rejected
    first = await api.post(
        "/api/v1/auth/refresh", json={"refresh_token": tokens["refresh_token"]}
    )
    assert first.status_code == 200
    reuse = await api.post(
        "/api/v1/auth/refresh", json={"refresh_token": tokens["refresh_token"]}
    )
    assert reuse.status_code == 401

    # 7. Logout revokes the current refresh token
    new_refresh = first.json()["refresh_token"]
    assert (
        await api.post("/api/v1/auth/logout", json={"refresh_token": new_refresh})
    ).status_code == 200
    after = await api.post(
        "/api/v1/auth/refresh", json={"refresh_token": new_refresh}
    )
    assert after.status_code == 401


@pytest.mark.asyncio
async def test_duplicate_registration_rejected(api, sent_emails):
    assert (await _register(api)).status_code == 201
    assert (await _register(api)).status_code == 409


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "bad_password",
    ["short1A", "alllowercase123", "ALLUPPERCASE123", "NoDigitsHereAtAll"],
)
async def test_weak_passwords_rejected(api, sent_emails, bad_password):
    response = await _register(api, {**REGISTER, "password": bad_password})
    assert response.status_code == 422
    assert sent_emails == []


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "bad_email", ["not-an-email", "a@b", "@example.com", "x y@example.com"]
)
async def test_invalid_emails_rejected(api, sent_emails, bad_email):
    response = await _register(api, {**REGISTER, "email": bad_email})
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_wrong_code_does_not_verify(api, sent_emails):
    await _register(api)
    _, code = sent_emails[0]
    wrong = "000000" if code != "000000" else "111111"
    assert (await _verify(api, wrong)).status_code == 400
    assert (await _login(api)).status_code == 401  # still unverified


@pytest.mark.asyncio
async def test_code_locked_after_five_wrong_attempts(api, sent_emails):
    await _register(api)
    _, code = sent_emails[0]
    wrong = "000000" if code != "000000" else "111111"
    for _ in range(5):
        assert (await _verify(api, wrong)).status_code == 400
    # Even the CORRECT code is refused once the attempt limit is reached
    locked = await _verify(api, code)
    assert locked.status_code == 400
    assert (await _login(api)).status_code == 401


@pytest.mark.asyncio
async def test_expired_code_rejected(api, sent_emails):
    await _register(api)
    _, code = sent_emails[0]
    await _sql(
        "UPDATE email_verification_codes SET expires_at=:t",
        t=datetime.now(timezone.utc) - timedelta(minutes=1),
    )
    assert (await _verify(api, code)).status_code == 400
    assert (await _login(api)).status_code == 401


@pytest.mark.asyncio
async def test_code_cannot_be_reused(api, sent_emails):
    await _register(api)
    _, code = sent_emails[0]
    assert (await _verify(api, code)).status_code == 200
    assert (await _verify(api, code)).status_code == 400


@pytest.mark.asyncio
async def test_resend_cooldown_then_new_code_replaces_old(api, sent_emails):
    await _register(api)
    _, old_code = sent_emails[0]
    payload = {"email": EMAIL}

    # Immediate resend is blocked by the 60-second cooldown
    blocked = await api.post("/api/v1/auth/resend-verification", json=payload)
    assert blocked.status_code == 400
    assert len(sent_emails) == 1

    # Simulate 2 minutes passing, then resend works
    await _sql(
        "UPDATE email_verification_codes SET created_at=:t",
        t=datetime.now(timezone.utc) - timedelta(minutes=2),
    )
    ok = await api.post("/api/v1/auth/resend-verification", json=payload)
    assert ok.status_code == 200
    assert len(sent_emails) == 2
    new_code = sent_emails[1][1]

    # The old code no longer works (unless the two codes happen to match)
    if old_code != new_code:
        assert (await _verify(api, old_code)).status_code == 400
    assert (await _verify(api, new_code)).status_code == 200
    assert (await _login(api)).status_code == 200


@pytest.mark.asyncio
async def test_wrong_password_and_unknown_user_look_identical(api, sent_emails):
    await _register(api)
    _, code = sent_emails[0]
    await _verify(api, code)
    wrong_pw = await _login(api, password="Wrong-Passw0rd-Test")
    unknown = await _login(api, email="ghost@example.com")
    assert wrong_pw.status_code == unknown.status_code == 401
    assert wrong_pw.json() == unknown.json()


@pytest.mark.asyncio
async def test_email_failure_returns_503_and_leaves_no_account(
    api, monkeypatch
):
    def broken_send(recipient_email, verification_code):
        raise RuntimeError("simulated email provider outage")

    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email", broken_send
    )
    response = await _register(api)
    # A provider outage is a service problem, not a duplicate-account conflict
    assert response.status_code == 503
    count = (await _sql("SELECT count(*) FROM users")).scalar_one()
    assert count == 0  # rolled back: user can simply try again later


@pytest.mark.asyncio
async def test_email_failure_then_retry_succeeds(api, monkeypatch):
    def broken_send(recipient_email, verification_code):
        raise RuntimeError("simulated email provider outage")

    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email", broken_send
    )
    assert (await _register(api)).status_code == 503

    outbox = []
    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email",
        lambda to, code: outbox.append((to, code)),
    )
    assert (await _register(api)).status_code == 201
    assert len(outbox) == 1


@pytest.mark.asyncio
async def test_resend_email_failure_returns_503(api, sent_emails, monkeypatch):
    await _register(api)
    await _sql(
        "UPDATE email_verification_codes SET created_at=:t",
        t=datetime.now(timezone.utc) - timedelta(minutes=2),
    )

    def broken_send(recipient_email, verification_code):
        raise RuntimeError("simulated email provider outage")

    monkeypatch.setattr(
        "app.services.auth_service.send_verification_email", broken_send
    )
    response = await api.post(
        "/api/v1/auth/resend-verification", json={"email": EMAIL}
    )
    assert response.status_code == 503
