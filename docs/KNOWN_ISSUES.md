# KNOWN ISSUES - AGGREY SHIELD

Last updated: 2026-11-03. Evidence comes from the 2026-11-03 session unless stated.

## Fixed (local only)

- Windows temp-file bug in app/services/intel/cli.py: mkstemp left a file handle open and
  Windows refused to delete the file (WinError 32). 4 tests failed. Fixed in commit 75643d7
  (NamedTemporaryFile, handle closed). NOT pushed yet, so GitHub CI has not seen it.

## Open

1. KI-1 Android and backend do not match (backend side CONFIRMED).
   Backend uses /scans/url, /scans/message, /scans (Android expects scan/url, scan/message,
   scan/history). Backend returns verdict, verdict_label, risk_score, text confidence,
   limitations; Android expects risk_level, classification, decimal confidence. Profile field
   is is_verified (Android expects email_verified). The Android code has not been read yet.
2. KI-2 Android build status unknown. Never compiled or run on a device.
3. KI-3 Email sender is still the Resend test sender (EMAIL_FROM is onboarding@resend.dev,
   key is set). Emails reach only the Resend account owner, so other people cannot register.
   A verified sending domain is needed. Domain application status unknown.
4. KI-4 Scanner quality is not measured on this machine. Historical numbers: precision 98.9
   percent, recall 8.9 percent, false alarm rate 0.1 percent. The evaluation test is skipped.
   Do not claim production-grade detection.
5. KI-5 False alarm and brand-list items from the handover (lockhosts.com,
   paypal-communication.com, member-hsbc-group.com, missing gmail alias) not reviewed.
6. KI-6 Threat lists were refreshed on 2026-11-02 (UTC) and are fresh now. They go stale after
   72 hours. The hourly Windows task (scripts/windows/schedule-feed-refresh.ps1) is not created.
7. KI-7 The Blogger reset page used a localhost backend URL (not tested again). The reset
   worked here by sending the token from the emailed link straight to the API. A public HTTPS
   backend and correct CORS are still needed.
8. KI-8 The system default python is 3.14.4. The project .venv uses 3.13.0. Do not run the
   project with the default python. The Python 3.14 move is not planned yet.
9. KI-9 Dependabot pull requests not inspected. The SQLAlchemy 2.1.6 upgrade failed CI before
   and must not be merged without investigation.
10. config.py line 25 has a default database password (changeme). Not checked whether .env
    overrides it. Verify and make the app refuse weak defaults.
11. Access tokens still work after logout until they expire. expires_in value not read.
12. The handover says PostgreSQL runs in Docker. On this PC it is the Windows service
    postgresql-x64-18. Docker Desktop was closed.
13. PROJECT_HANDOFF.md section 2 still says commit 05cccd8. Repo HEAD was 9b6b804.
14. Legacy scripts test_auth_api.py and test_auth_registration.py sit in backend/ and were not run.
15. tests/test_health.py was not read in detail (it passes in the full suite).
16. Warning: httpx with starlette.testclient is deprecated (install httpx2 later).
17. JWT secret strength, branch protection, runtime image contents (pip, setuptools), Trivy scan: unverified.

## Data in the real database (aggreyshield)

- 11 users in total. Only the test account was looked at. It is verified, MFA off, created
  2026-10-17. Its password was reset on 2026-11-03 through the live reset flow.
- The test account has 8 scans, 3 of them created on 2026-11-03.

## Notes

- API description: /api/v1/openapi.json (also /api/v1/docs and /api/v1/redoc).
- Local server command used: python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
  (the README uses --host 0.0.0.0 --reload).
- A separate test database aggreyshield_test exists and holds only test data.
- An unrelated file GROUP_8_SUBMISSION...pptx was uploaded to the chat and not opened.
