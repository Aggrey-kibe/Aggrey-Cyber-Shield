# PROJECT_HANDOFF.md — AGGREY SHIELD

> Checkpoint status: last confirmed pushed commit is a91f3f5 (rate limiting; confirmed by push output `96f33e2..a91f3f5`).
> The Session 3 scanning patch is committed only once the owner confirms its push.
> This file itself is committed only once it is added to the repo root and pushed.
> Source of truth for code: https://github.com/Aggrey-kibe/Aggrey-Cyber-Shield
> Founder/developer: Aggrey Kibe Kwamboka (Kenya). Android ID: com.aggreyshield.app

## 1. Vision (short)
Cybersecurity platform: phishing URL/domain detection, message analysis, explainable threat scores,
auth, scan history, alerts, incidents, training, RBAC/org isolation, Android-first, then web dashboard.
No fake detection, no hardcoded scores, no simulated auth.

## 2. Verified state (GitHub `main` @ 05cccd8; earlier: 450e56b resend pin)
Branch: `main`, in sync with `origin/main`. Verified from a fresh clone of GitHub (NOT the author's local PC).

| Item | Evidence |
|---|---|
| Stack: FastAPI 0.115.6, SQLAlchemy 2.0.36 async, asyncpg, Alembic 1.14.1, argon2-cffi, PyJWT, pyotp | `backend/requirements.txt` |
| App imports successfully when `resend` is installed | `import app.main` -> OK (dummy env values) |
| App FAILS to import without `resend` | `ModuleNotFoundError: No module named 'resend'` (reproduced) |
| Unit tests | `pytest` -> **6 passed** (health/foundation tests only) |
| 15 routes registered | `/health`, `/api/v1/health`, 9 auth routes, docs |
| No secrets in git history | `.env`, keys, dumps never committed; `.gitignore` ignores `.env`, `.env.*` |

Auth routes registered: register, verify-email, resend-verification, forgot-password, reset-password,
change-password, login, refresh, logout (all `POST /api/v1/auth/...`).

## 3. Unverified / not working
- Auth flows have **no automated tests**. The 2 root-level files `backend/test_auth_api.py` and
  `backend/test_auth_registration.py` are manual scripts (need a live server on :8001 / a live DB with
  specific seeded users). They are outside `testpaths = tests`, and fail at collection if collected.
- Docker: image never confirmed rebuilt/running after the `resend` fix.
- Resend email delivery: HTTP 409 never diagnosed.
- End-to-end register + verify with a fresh account: VERIFIED live on 2026-10-17 for the Resend owner's address (see Session 1 live test).
- Password reset page on Blogger calls localhost: needs a public HTTPS backend.
- Rate limiting: implemented and pushed (a91f3f5); in-memory, single process. Multiple workers/containers each keep
  their own counters (move to Redis then).
- Scanning (Session 3): heuristic only. No live threat-intelligence lookups, no page fetching. NOT yet run on the owner's PC.
- Missing features: incidents, alerts, training, threat-intelligence lookups, organisation/RBAC. (`/users/me`, URL/message
  scanning and scan history were added in Session 3.)

## 4. Confirmed blockers / defects
1. **`resend` missing from `backend/requirements.txt` on GitHub** (root cause of Docker `ModuleNotFoundError`).
   Local PC has an uncommitted/staged fix; GitHub does not. Version installed during this check: resend 2.49.1
   (pin whichever version your local venv uses: `pip show resend`).
2. `backend/.env.example` is stale: lists only Phase 1 vars. Settings that are **required with no default**:
   `JWT_SECRET_KEY`, `RESEND_API_KEY`. Also used: `JWT_ALGORITHM`, `ACCESS_TOKEN_EXPIRE_MINUTES`,
   `REFRESH_TOKEN_EXPIRE_DAYS`, `MFA_ISSUER_NAME`, `PASSWORD_RESET_URL_BASE`.
3. `requirements.txt` header comment and README status table say "Phase 1" but auth is implemented.
4. Default `DATABASE_URL` in `config.py` contains placeholder password `changeme` (acceptable for dev only).
5. Emails are sent from `onboarding@resend.dev` (Resend sandbox sender; restricted delivery). A verified
   domain is needed before real users. Not yet confirmed as cause of the 409.
6. Dockerfile has a stale "NOT YET BUILD-TESTED" header and does not need `alembic` files copied only
   if migrations run elsewhere (decide at deploy time).

## 5. Environment variable NAMES (values never stored here)
APP_NAME, ENVIRONMENT, DEBUG, API_V1_STR, DATABASE_URL, CORS_ALLOWED_ORIGINS, LOG_LEVEL,
JWT_SECRET_KEY*, JWT_ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES, REFRESH_TOKEN_EXPIRE_DAYS,
MFA_ISSUER_NAME, RESEND_API_KEY*, PASSWORD_RESET_URL_BASE, EMAIL_FROM, TRUST_PROXY_HEADERS.  (no new variables in Session 3)   (* = required, no default)

## 6. Migrations (Alembic, `backend/alembic/versions/`)
be3e4976da87 users -> cda9f895f46d refresh_tokens -> 4db34f20cfb4 email_verification_codes
-> 9f105e83566d password_reset_tokens -> ad74ffee848f scans (Session 3; owner must run `alembic upgrade head`).
Chain verified in a sandbox: upgrade, downgrade, re-upgrade, no model/migration drift, from-empty = 6 tables.

## 7. Local services (development only, per handoff)
Backend dev server: port 8001. Docker test mapping: host 8002 -> container 8000. Not production.

## 8. Decisions
- Inspected the GitHub clone only; local-PC-only changes were not visible and must be reconciled.
- Fix `resend` dependency before any further Docker work.

## 9. (superseded by section 13) previous next task
Commit the `resend` pin + refreshed `.env.example`, rebuild Docker, run container with env file,
confirm `GET /health` -> 200 and `GET /api/v1/health` reports DB status.
Then: add real automated auth tests (pytest + test DB), add rate limiting, implement `/users/me`.

Verification: `pytest` all green incl. new auth tests; `docker build` succeeds; container `/health` 200.

## 10. Production risks (open)
Rate limiting is single-process only; sandbox email sender; no HTTPS backend; no monitoring/backups; auth untested in CI.

## 11. SESSION LOG (append-only)

### Session 1
- Found `resend` missing from requirements (root cause of Docker failure). Pinned `resend==2.49.1`,
  committed & pushed as 450e56b (confirmed: `f19113a..450e56b main -> main`).
- Docker image rebuilt with `--no-cache`; container started, logs showed `Application startup complete`,
  `GET /health -> 200` on host port 8002. (Not yet tested with a database from inside the container.)
- Local DB: `alembic current` and `alembic heads` both `9f105e83566d (head)`.
- Dev server restarted on 127.0.0.1:8001; `/api/v1/health -> 200` (DB reachable).
- Negative auth checks on live server: wrong login 401; change-password w/o token 401;
  forgot-password unknown email 200 generic message; unknown route 404; security headers present.
- Local account `aggreykibe62@gmail.com` already exists and is verified (register -> 409 duplicate;
  resend-verification -> 400 "already verified").
- Forgot-password email DID arrive in Gmail (delivery works for the Resend account owner's address).
  Reset page on Blogger then showed "Unable to connect" (expected: page cannot reach localhost).
- Finding: the earlier "409" was this API's own 409 (`RegistrationError` -> 409), not necessarily Resend's.
  It covered both "email already registered" and any email/commit failure.

### Session 1 (cont.) — registration hardening (COMMITTED 05cccd8, pushed: `450e56b..05cccd8 main -> main`)
Verified in a sandbox with real PostgreSQL 16, migrations applied from scratch (4 migrations -> 5 tables OK).
- New `tests/test_registration_flow.py` (+ DB fixtures in `tests/conftest.py`): 25 tests pass, stable over 3 runs.
  DB tests refuse to run unless DB name ends with `_test`. Only the outgoing email call is stubbed in tests.
  Covered: register -> hashed password/code -> blocked login -> verify -> login -> refresh rotation + reuse
  rejection -> logout revocation; duplicate; weak password/invalid email; wrong code; 5-attempt lockout;
  expired code; code reuse; resend cooldown + old-code invalidation; identical wrong-pw/unknown-user replies;
  email outage -> 503 + no orphan account + retry works.
- Fixes: provider failure now returns 503 (was 409); email send moved off the event loop (`asyncio.to_thread`);
  sender is now setting `EMAIL_FROM`; `.env.example` lists all required variable names.
- Real (unstubbed) email path with provider unreachable -> 503, 0 users left in DB.

## 12. OPEN ITEMS (honest status)
1. **New members other than the Resend account owner cannot receive email** until a domain is verified in Resend
   and `EMAIL_FROM` uses it (Blogger domain cannot be used; a purchased domain is needed). Not yet done.
2. Real Resend delivery of the *verification code* email to a brand-new address: NOT yet verified end to end
   (only the password-reset email was seen arriving, to the owner's address).
3. Rate limiting is in-memory per process (see core/rate_limit.py); per-email lockout can be abused to temporarily
   lock a known victim email (accepted trade-off, 15 min, cleared on successful login). Needs Redis for multi-worker.
4. Production password reset needs a public HTTPS backend + CORS for the Blogger origin.
5. Register and resend-verification reveal whether an email is registered (409 / "already verified");
   accepted trade-off for now, revisit.
6. `test_auth_api.py` / `test_auth_registration.py` in `backend/` are manual scripts; consider removing/moving.
7. Docker container never tested against a database; not production-ready.
8. Android integration untested.

## 13. NEXT EXACT TASK
1. Apply + push the scanning patch; run `alembic upgrade head` on the dev DB; restart the dev server; live-test /scans.
2. Run the DB tests locally against a dedicated `aggreyshield_test` database (they skip otherwise).
3. GitHub Student Pack was still PENDING (2026-10-18). When approved: claim a free domain (Namecheap .me), verify it in
   Resend, set EMAIL_FROM. Until then registration works only for the Resend owner's address.
4. Next features: threat-intelligence lookups (hash-based, no URL fetching), alerts, incidents, then Android wiring.

### Session 1 live test (owner's PC, 2026-10-17)
- Deleted the local test account for the Resend owner's address (cascade removes its tokens/codes), re-registered on
  the new server (port 8003): API returned "Account created", which only happens after Resend accepted the email.
- Verification code email arrived. Login BEFORE verifying -> 401 "Please verify your email address before signing in."
  Verify with the emailed code -> success. Login AFTER -> access + refresh tokens received. (Owner's address only:
  the Resend sandbox sender cannot deliver to other addresses.)

### Session 2 — rate limiting (patch built and verified in a sandbox with real PostgreSQL; not yet pushed)
- New `app/core/rate_limit.py`: sliding-window limiter (no new dependency), per-IP limits (generous: shared mobile
  IPs) + per-email limits (strict). Login: 10/15min per email (cleared on successful login), 60/15min per IP.
  Register 5/h per email + 20/h per IP. Verify 10/15min per email. Resend-verification 5/h per email.
  Forgot-password 5/h per email (same behaviour for known and unknown emails). Reset-password and change-password per IP.
- Blocked requests: HTTP 429, `Retry-After` header, standard error envelope (`TOO_MANY_REQUESTS`).
- `TRUST_PROXY_HEADERS` (default false): X-Forwarded-For is ignored unless exactly one trusted proxy (ALB) is in front;
  then the LAST entry is used. Forged headers cannot bypass limits when disabled (tested).
- Error handler now forwards exception headers (Retry-After, and WWW-Authenticate on 401, previously dropped).
- Memory capped (100k keys, expired keys pruned).
- Tests: 43 pass (25 earlier + 18 new), stable over repeated runs; mutation check (limiter disabled) makes 8 tests fail.
  Real HTTP check on a live uvicorn: attempts 1-10 -> 401, attempt 11 -> 429 with `retry-after: 900`.
- Test safety: autouse fixture blocks both real email functions in all tests (before this fix the new forgot-password
  tests attempted real Resend calls: 50 attempts logged; after: 0).

### Session 3 — threat-scanning core + /users/me (patch built and verified in a sandbox with real PostgreSQL; not yet pushed)
- NEW `app/services/scanning/` (pure, offline, never opens/fetches anything, so no SSRF):
  `url_analyzer.py`, `message_analyzer.py`, `brands.py` (editable data: brands + official domains, shorteners, free hosts,
  risky TLDs, keywords), `types.py` (Finding, scoring, verdict wording, limitations).
- URL signals: dangerous scheme (javascript:/data:), '@' credential trick, raw/obfuscated IP, punycode/mixed-script look-alikes,
  brand impersonation (look-alike chars, 1-char typos, brand in sub-name/domain, weak 'prefix' match), shorteners, free hosting,
  odd ports/TLDs, many subdomains/hyphens, redirect params, .apk/.exe downloads, encoding tricks, no-HTTPS, long URLs.
- Message signals (English + some Swahili, Kenyan M-PESA scams): asks PIN/OTP/password (negations like "never share" ignored),
  "sent by mistake, send back", prize claims, advance fees (loans/jobs), investment promises, account threats, verify prompts,
  app install, urgency, payment requests, pressure+link combination; every link inside is URL-analysed.
- Scores 0-100 -> verdicts: no_warning_signs / low_risk / suspicious / likely_dangerous. The system NEVER says "safe";
  every result carries `confidence: "limited"` and `limitations` text. Every finding has a plain-language explanation.
- New endpoints (all need a signed-in user): POST /api/v1/scans/url, POST /api/v1/scans/message, GET /api/v1/scans
  (paginated, ?scan_type=), GET/DELETE /api/v1/scans/{id}, GET /api/v1/users/me.
- Data/privacy: table `scans` (migration ad74ffee848f). Stored preview drops URL query/fragment and masks digit runs of 4+
  in messages; SHA-256 of the input kept; users can delete their scans; deleting a user cascades. Other users' scans return 404.
- Rate limits: 30 scans/min per user, 300 per 15 min per IP.
- New dependency: `tldextract==5.4.0` (public-suffix data bundled; used offline, `cache_dir=None`, so no network or disk writes).
- Tests: 139 pass (43 earlier + 96 new: analyzers incl. 1,500-input fuzz and hostile inputs; API incl. isolation/privacy).
  Mutation checks: removing the per-user filter (get and list) fails the isolation tests; disabling brand detection fails 10 tests.
- KNOWN LIMITS: heuristic rules only; no reputation feeds; brand list is small and must be reviewed/extended; Swahili coverage is
  partial; very new scam styles can look clean. Do not market this as complete protection.
