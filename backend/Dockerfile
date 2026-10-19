# Aggrey Shield backend — Dockerfile
#
# Status: IMPLEMENTED, NOT YET BUILD-TESTED.
# Full Docker build/run verification (with Postgres + Redis via
# docker-compose) happens in Phase 12. This file is syntactically correct
# and follows standard security practice, but has not yet been run in this
# project's own CI — do not treat it as "tested" until Phase 12 confirms it.

# ---- Stage 1: build dependencies in an isolated layer ----
FROM python:3.14-slim AS builder

WORKDIR /build

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# ---- Stage 2: slim runtime image ----
FROM python:3.14-slim

# Runtime needs libpq for asyncpg's C extension parts / psycopg-style linking.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libpq5 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system appgroup \
    && useradd --system --gid appgroup --create-home appuser

WORKDIR /app

COPY --from=builder /root/.local /home/appuser/.local
COPY ./app ./app

RUN chown -R appuser:appgroup /app
USER appuser

ENV PATH=/home/appuser/.local/bin:$PATH \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health').read()" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
