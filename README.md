# AGGREY SHIELD

**Brand:** AGGREY CYBER SHIELD
**Developer:** Aggrey Kibe Kwamboka — Kenya
**GitHub:** https://github.com/Aggrey-kibe
**Website:** https://aggreykibe.blogspot.com/

A cybersecurity, fraud-detection, digital-safety and incident-management platform
(Android app, web dashboard, secure API, PostgreSQL, AI/ML analysis).

This archive contains **Phase 1: Backend Foundation**. Later phases (database
models and migrations, authentication and RBAC, incidents, threat intelligence,
AI/ML, web dashboard, Android app, and so on) are added on top of this
foundation without changing it.

---

## Status of Phase 1

| Item | Status |
|---|---|
| FastAPI application, versioned API (`/api/v1`) | IMPLEMENTED and TESTED |
| `GET /health` liveness endpoint | IMPLEMENTED and TESTED |
| `GET /api/v1/health` readiness endpoint (checks database) | IMPLEMENTED and TESTED |
| Pydantic settings loaded from `.env` | IMPLEMENTED and TESTED |
| Security headers middleware | IMPLEMENTED and TESTED |
| CORS with explicit origin allow-list | IMPLEMENTED and TESTED |
| Request IDs and request logging | IMPLEMENTED and TESTED |
| Consistent error envelope, no leaked internals in production | IMPLEMENTED and TESTED |
| Async PostgreSQL engine and session factory | IMPLEMENTED (connection logic exercised; see next row) |
| A running PostgreSQL server | REQUIRES CONFIGURATION (not needed for Phase 1 tests) |
| Dockerfile | IMPLEMENTED, NOT YET BUILD-TESTED (verified in Phase 12) |
| ORM models and Alembic migrations | NOT YET IMPLEMENTED (Phase 3) |
| Authentication, RBAC, rate limiting | NOT YET IMPLEMENTED (Phase 4) |

Verified on Python 3.12 (Linux) with the pinned versions in `backend/requirements.txt`.
The same commands below were written for Windows 11 + PowerShell but have not
been run on a Windows machine by the author of this archive.

---

## Requirements

- Windows 11
- Python 3.11, 3.12 or 3.13 (3.12 recommended). Check with `python --version`.
- Visual Studio Code (optional but recommended)

You do **not** need PostgreSQL, Redis or Docker to install, test and run Phase 1.

---

## Install (Windows PowerShell)

Extract `aggrey-shield.zip`, then open PowerShell in the extracted folder
(the folder that contains `backend` and this README).

```powershell
# Open the project in VS Code (optional)
code .

# Go to the backend folder
cd backend

# Create a virtual environment
python -m venv .venv

# Allow the activation script to run in this PowerShell window only
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# Activate the virtual environment
.venv\Scripts\Activate.ps1

# Install dependencies
python -m pip install --upgrade pip
pip install -r requirements-dev.txt

# Create your local environment file from the template
Copy-Item .env.example .env
```

If `python -m venv .venv` fails or `pip install` reports build errors, you are
probably on a Python version newer than 3.13. Install Python 3.12 and use
`py -3.12 -m venv .venv` instead.

---

## Run the tests

From the `backend` folder with the virtual environment active:

```powershell
python -m pytest -v
```

Expected result: every test passes. Tests that need a database are skipped unless `DATABASE_URL` points to a database whose name ends in `_test` (they delete data, so they refuse to touch anything else).

---

## Start the server

From the `backend` folder with the virtual environment active:

```powershell
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Look for `Application startup complete.` in the output.

## Check that it works

Open a **second** PowerShell window while the server is running:

```powershell
# Liveness: always 200, no database needed
Invoke-RestMethod http://localhost:8000/health

# Readiness: 200 if PostgreSQL is reachable, 503 with "database": "unavailable" if not.
# Without PostgreSQL running, the 503 response is the correct Phase 1 behavior.
Invoke-RestMethod http://localhost:8000/api/v1/health

# Security headers and request ID
curl.exe -i http://localhost:8000/health

# Consistent error envelope
Invoke-RestMethod http://localhost:8000/api/v1/nonexistent
```

Interactive API documentation: http://localhost:8000/api/v1/docs
(disabled automatically when `ENVIRONMENT=production`).

Stop the server with `Ctrl+C`.

---

## Configuration (`backend/.env`)

| Variable | Default | Purpose |
|---|---|---|
| `APP_NAME` | `Aggrey Shield` | Application name shown in responses and docs |
| `ENVIRONMENT` | `development` | `development`, `staging` or `production` |
| `DEBUG` | `true` | Debug flag |
| `API_V1_STR` | `/api/v1` | Prefix for the versioned API |
| `DATABASE_URL` | `postgresql+asyncpg://aggreyshield:changeme@localhost:5432/aggreyshield` | PostgreSQL connection string (replace the placeholder password) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of allowed browser origins |
| `LOG_LEVEL` | `INFO` | `DEBUG`, `INFO`, `WARNING`, `ERROR` or `CRITICAL` |

`.env` is listed in `.gitignore`. Never commit real secrets.
Only `.env.example` (placeholders) belongs in Git.

---

## Keeping the threat lists fresh

AGGREY SHIELD checks scanned links against public phishing lists (Phishing.Database, MIT licence). The lists change daily, so they
must be refreshed. They are loaded by a separate command, never inside the web server (loading takes minutes and would slow the API).

```powershell
# From the backend folder, with the virtual environment active:
python -m app.services.intel.cli refresh --all --if-stale   # reloads only lists older than 20 hours; ~1 second when fresh
python -m app.services.intel.cli status                     # what is loaded, how old, recent runs
```

**Windows (development):** register an hourly task once (runs as you, only while you are logged in, catches up after sleep):

```powershell
.\scripts\windows\schedule-feed-refresh.ps1             # register
Start-ScheduledTask -TaskName AggreyShield-FeedRefresh   # optional: run it now
.\scripts\windows\schedule-feed-refresh.ps1 -Remove     # remove it again
```

Output goes to `%LOCALAPPDATA%\AggreyShield\feed-refresh.log`.

**Production (AWS, not deployed yet):** run the same image as a scheduled ECS task (EventBridge, hourly) with the command
`python -m app.services.intel.cli refresh --all --if-stale`. A database lock guarantees two refreshes never overlap.

**Is it working?** `GET /api/v1/health` reports `threat_intel`: `fresh`, `incomplete` (not every list loaded), `stale`
(a list is older than 72 hours) or `not_loaded`. Stale or missing lists do not stop the service, but protection is weaker, and
the server logs a warning (at most once an hour). Alert on that status or log line.

## Docker (optional, not yet build-tested)

From the `backend` folder:

```powershell
docker build -t aggrey-shield-backend .
docker run --rm -p 8000:8000 --env-file .env aggrey-shield-backend
```

Inside a container, `localhost` refers to the container itself, so the readiness
endpoint will report the database as unavailable until Phase 12 adds
`docker-compose.yml` with PostgreSQL and Redis.

---

## Project layout

```
aggrey-shield/
├── .gitignore
├── README.md
└── backend/
    ├── .dockerignore
    ├── .env.example
    ├── Dockerfile
    ├── pytest.ini
    ├── requirements.txt          # runtime packages (what Docker installs)
    ├── requirements-dev.txt      # + test tools
    ├── app/
    │   ├── __init__.py
    │   ├── main.py                  # FastAPI app, middleware, routers
    │   ├── core/
    │   │   ├── __init__.py
    │   │   ├── config.py            # Pydantic settings
    │   │   ├── exceptions.py        # Error envelope and handlers
    │   │   ├── logging_config.py    # Dev text logs, production JSON logs
    │   │   └── middleware.py        # Security headers, request IDs
    │   ├── api/
    │   │   ├── __init__.py
    │   │   └── v1/
    │   │       ├── __init__.py
    │   │       └── health.py        # GET /api/v1/health
    │   └── db/
    │       ├── __init__.py
    │       └── session.py           # Async PostgreSQL engine and sessions
    └── tests/
        ├── __init__.py
        ├── conftest.py
        └── test_health.py
```

---

## Troubleshooting

- **"running scripts is disabled on this system"** when activating the virtual
  environment: run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`
  and activate again.
- **`ModuleNotFoundError: No module named 'app'`**: run pytest and uvicorn from
  the `backend` folder, not from the project root.
- **Port 8000 already in use**: start on another port, for example
  `uvicorn app.main:app --reload --port 8001`.
- **`/api/v1/health` returns 503**: expected until a PostgreSQL server matching
  `DATABASE_URL` is running. The API itself is healthy; check `GET /health`.
