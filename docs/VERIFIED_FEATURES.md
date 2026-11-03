# VERIFIED FEATURES - AGGREY SHIELD

Last updated: 2026-11-03. Git: local commit 75643d7 (not pushed) on top of 9b6b804.
Environment: Windows 11, project .venv with Python 3.13.0, PostgreSQL 18 (Windows service).

Rule: a feature is "verified" only when a test or a live call showed it working.
Source code alone does not count.

## 1. Automated tests (database: aggreyshield_test)

- Full suite: 221 passed, 1 skipped, 0 failed (after the Windows fix in commit 75643d7).
- The skipped test is tests/test_evaluation_regression.py:123. It needs the external
  feeds (python -m evaluation.fetch_external), so the scanner-quality numbers were NOT re-measured.
- To run the database tests: point DATABASE_URL at a database whose name ends in _test,
  and set CI_REQUIRE_DB_TESTS=1. The tests empty users, threat_feed_runs and threat_indicators there.

## 2. Live checks (real database aggreyshield, server on 127.0.0.1:8000, 2026-11-03)

| Feature | Evidence | Known limits |
|---|---|---|
| Server start | uvicorn started, application startup complete | development mode only |
| Health | GET /api/v1/health returned 200, database connected | overall status can be ok while lists are stale (design question) |
| Anonymous access refused | 401 on /users/me, /scans, POST /scans/url, fake token, /intel/status | |
| Threat-list refresh | refresh --all --if-stale exit 0; domains 391,932 and links 788,716 accepted; health became fresh, 2 of 2 sources | not scheduled yet; stale again after 72 hours |
| Registration checks | empty fields returned 422; existing email returned 409 | a NEW registration was not tested live |
| Password reset | forgot-password, email received, reset-password returned 200 | single use and 15 minute expiry not tested live; Blogger reset page not tested (KI-7) |
| Login | 200 with access_token, refresh_token, token_type, expires_in | wrong-password path returned 401 (generic message) |
| /users/me | 200 with id, email, full_name, is_verified, mfa_enabled, created_at | |
| Refresh rotation | refresh 200; reuse of the old token 401 | |
| Logout | 200; refresh after logout 401 | old access token still returned 200 after logout |
| URL scan | 201; example.com: no_warning_signs, risk 0, confidence limited, 3 limitations; invented PayPal-style address: likely_dangerous, risk 74, 4 findings | rules only; 2 invented examples prove nothing about accuracy |
| Message scan | 201; invented scam text: likely_dangerous, risk 100, 5 findings | same limit |
| Scan history | 200 with items, total, limit, offset; total 8 for the test account | |

## 3. NOT verified yet

- Creating a brand new account and email verification (only an existing verified account was used)
- One user not being able to read another user's scans (covered by automated tests only)
- DELETE /api/v1/scans/{scan_id}, change-password, resend-verification
- MFA (columns exist, login accepts mfa_code, but there are no MFA endpoints)
- Detection accuracy (precision 98.9 percent, recall 8.9 percent are historical, not re-measured)
- Android build, login, scanning and history
- Web dashboard, Docker image, CI run for commit 75643d7, pushing to GitHub

## 4. Endpoints that do NOT exist in the backend (live API list)

MFA, organisations, incidents, alerts, notifications, training, security score and
threat indicators. The Android app has screens for these.
