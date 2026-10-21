"""Keeping the threat lists fresh: health reporting, `refresh --if-stale`, and the no-overlap lock."""
import dataclasses
import logging

import pytest
from sqlalchemy import text

from app.api.v1 import health as health_module
from app.db.session import AsyncSessionLocal, engine
from app.services.intel import cli
from app.services.intel.feeds import FEEDS
from app.services.intel.health import intel_health
from app.services.intel.ingest import ingest_file

LINKS_NAME = "phishing_database_links"
DOMAINS_NAME = "phishing_database_domains"
LINK_LINES = ["http://a1.example-phish.xyz/x", "http://a2.example-phish.xyz/x", "http://a3.example-phish.xyz/x"]
DOMAIN_LINES = ["a1.example-phish.top", "a2.example-phish.top", "a3.example-phish.top"]


def _small(name):
    return dataclasses.replace(FEEDS[name], min_accepted=3)


async def _load(tmp_path, name, lines):
    path = tmp_path / f"{name}.txt"
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    async with AsyncSessionLocal() as db:
        return await ingest_file(db, _small(name), path, sha256="a" * 64, size=path.stat().st_size)


async def _age_runs(hours):
    async with AsyncSessionLocal() as db:
        await db.execute(text(f"UPDATE threat_feed_runs SET finished_at = now() - interval '{hours} hours'"))
        await db.commit()


# ------------------------------------------------------------------ health summary
@pytest.mark.asyncio
async def test_health_summary_walks_through_every_state(clean_db, tmp_path):
    async with AsyncSessionLocal() as db:
        assert (await intel_health(db)) == {
            "status": "not_loaded", "sources_loaded": 0, "sources_expected": 2, "oldest_refresh_age_hours": None}
    await _load(tmp_path, LINKS_NAME, LINK_LINES)
    async with AsyncSessionLocal() as db:
        summary = await intel_health(db)
    assert summary["status"] == "incomplete" and summary["sources_loaded"] == 1
    await _load(tmp_path, DOMAINS_NAME, DOMAIN_LINES)
    async with AsyncSessionLocal() as db:
        assert (await intel_health(db))["status"] == "fresh"
    await _age_runs(100)                                         # past the 72 h freshness limit
    async with AsyncSessionLocal() as db:
        summary = await intel_health(db)
    assert summary["status"] == "stale" and summary["oldest_refresh_age_hours"] >= 100


@pytest.mark.asyncio
async def test_readiness_endpoint_reports_threat_list_state_without_leaking_detail(api, tmp_path):
    body = (await api.get("/api/v1/health")).json()
    assert body["status"] == "ok" and body["threat_intel"]["status"] == "not_loaded"
    await _load(tmp_path, LINKS_NAME, LINK_LINES)
    await _load(tmp_path, DOMAINS_NAME, DOMAIN_LINES)
    response = await api.get("/api/v1/health")
    assert response.status_code == 200 and response.json()["threat_intel"]["status"] == "fresh"
    assert "http" not in str(response.json()["threat_intel"])     # counts and ages only, never indicators
    await _age_runs(100)
    response = await api.get("/api/v1/health")
    assert response.status_code == 200                              # stale lists do NOT make the service unready
    assert response.json()["status"] == "ok" and response.json()["threat_intel"]["status"] == "stale"


@pytest.mark.asyncio
async def test_readiness_still_503_with_unavailable_threat_intel_when_database_is_down(api, monkeypatch):
    async def down():
        return False
    monkeypatch.setattr(health_module, "check_database_connection", down)
    response = await api.get("/api/v1/health")
    assert response.status_code == 503
    assert response.json()["database"] == "unavailable" and response.json()["threat_intel"]["status"] == "unavailable"


@pytest.fixture
def health_log(monkeypatch):
    """Capture the health logger directly (the app's logging set-up replaces the root handlers that caplog uses)."""
    messages = []

    class _Collect(logging.Handler):
        def emit(self, record):
            messages.append(record.getMessage())

    handler = _Collect(level=logging.WARNING)
    logger = logging.getLogger("aggreyshield.health")
    logger.addHandler(handler)
    monkeypatch.setattr(health_module, "_last_intel_warning", None)
    yield messages
    logger.removeHandler(handler)


@pytest.mark.asyncio
async def test_not_loaded_warning_fires_even_when_the_host_has_only_just_booted(api, health_log, monkeypatch):
    # time.monotonic() counts from machine boot: simulate a host that has been up for only 5 minutes.
    monkeypatch.setattr(health_module, "_clock", lambda: 300.0)
    await api.get("/api/v1/health")
    assert sum("threat lists are not_loaded" in m for m in health_log) == 1


@pytest.mark.asyncio
async def test_stale_warning_is_logged_at_most_once_an_hour(api, health_log, monkeypatch):
    now = {"t": 10_000.0}
    monkeypatch.setattr(health_module, "_clock", lambda: now["t"])
    for _ in range(5):
        await api.get("/api/v1/health")                                # lists not loaded: warns, but only once
    assert len(health_log) == 1
    now["t"] += 3599
    await api.get("/api/v1/health")
    assert len(health_log) == 1                                        # 59 min 59 s later: still quiet
    now["t"] += 2
    await api.get("/api/v1/health")
    assert len(health_log) == 2                                        # an hour later: reminds again


@pytest.mark.asyncio
async def test_no_warning_when_lists_are_fresh(api, health_log, tmp_path):
    await _load(tmp_path, LINKS_NAME, LINK_LINES)
    await _load(tmp_path, DOMAINS_NAME, DOMAIN_LINES)
    await api.get("/api/v1/health")
    assert health_log == []


# ----------------------------------------------------------- refresh --if-stale + lock
@pytest.fixture
def fake_download(monkeypatch, tmp_path):
    """Replace the network download with a tiny local file; count how often it is called."""
    calls = []

    def _fake(url, dest, max_bytes):
        name = next(n for n, f in FEEDS.items() if f.url == url)
        lines = LINK_LINES if FEEDS[name].indicator_type == "url" else DOMAIN_LINES
        dest.write_text("\n".join(lines) + "\n", encoding="utf-8")
        calls.append(name)
        return dest.stat().st_size, "b" * 64

    monkeypatch.setattr(cli, "download", _fake)
    for name in FEEDS:
        monkeypatch.setitem(FEEDS, name, _small(name))
    return calls


@pytest.mark.asyncio
async def test_if_stale_downloads_only_when_a_list_is_missing_or_old(clean_db, fake_download):
    assert await cli._refresh([LINKS_NAME], None, if_stale=True) == 0
    assert fake_download == [LINKS_NAME]                              # never loaded -> refreshed

    assert await cli._refresh([LINKS_NAME], None, if_stale=True) == 0
    assert fake_download == [LINKS_NAME]                              # fresh -> skipped, no download

    await _age_runs(19)
    await cli._refresh([LINKS_NAME], None, if_stale=True)
    assert fake_download == [LINKS_NAME]                              # 19 h < 20 h interval -> still skipped

    await _age_runs(21)
    await cli._refresh([LINKS_NAME], None, if_stale=True)
    assert fake_download == [LINKS_NAME, LINKS_NAME]                  # 21 h >= 20 h -> refreshed

    await cli._refresh([LINKS_NAME], None, if_stale=False)            # without the flag it always refreshes
    assert fake_download == [LINKS_NAME] * 3


@pytest.mark.asyncio
async def test_refresh_all_if_stale_only_touches_what_needs_it(clean_db, fake_download):
    await cli._refresh(sorted(FEEDS), None, if_stale=True)
    assert sorted(fake_download) == sorted(FEEDS)
    fake_download.clear()
    async with AsyncSessionLocal() as db:                              # only the domains feed becomes old
        await db.execute(text("UPDATE threat_feed_runs SET finished_at = now() - interval '30 hours' WHERE source = :s"),
                         {"s": DOMAINS_NAME})
        await db.commit()
    await cli._refresh(sorted(FEEDS), None, if_stale=True)
    assert fake_download == [DOMAINS_NAME]


@pytest.mark.asyncio
async def test_a_second_refresh_cannot_overlap_a_running_one(clean_db, fake_download, capsys):
    async with engine.connect() as holder:                             # simulates a refresh already in progress
        got = (await holder.execute(text("SELECT pg_try_advisory_lock(:k)"), {"k": cli.LOCK_KEY})).scalar_one()
        assert got is True
        assert await cli._refresh(sorted(FEEDS), None, if_stale=False) == 0
        assert fake_download == []                                      # nothing downloaded while locked
        assert "already running" in capsys.readouterr().out
        await holder.execute(text("SELECT pg_advisory_unlock(:k)"), {"k": cli.LOCK_KEY})
        await holder.commit()
    assert await cli._refresh([LINKS_NAME], None, if_stale=False) == 0
    assert fake_download == [LINKS_NAME]                                # lock was released properly afterwards


@pytest.mark.asyncio
async def test_lock_is_released_even_if_a_refresh_crashes(clean_db, monkeypatch):
    async def boom(*a, **k):
        raise RuntimeError("simulated crash")
    monkeypatch.setattr(cli, "_refresh_locked", boom)
    with pytest.raises(RuntimeError):
        await cli._refresh([LINKS_NAME], None, if_stale=False)
    async with engine.connect() as probe:                              # the lock must be free again
        got = (await probe.execute(text("SELECT pg_try_advisory_lock(:k)"), {"k": cli.LOCK_KEY})).scalar_one()
        await probe.execute(text("SELECT pg_advisory_unlock(:k)"), {"k": cli.LOCK_KEY})
        await probe.commit()
    assert got is True


@pytest.mark.asyncio
async def test_failed_download_reports_failure_and_keeps_old_data(clean_db, fake_download, monkeypatch, tmp_path):
    await cli._refresh([LINKS_NAME], None)
    def refuse(url, dest, max_bytes):
        raise cli.DownloadError("simulated outage")
    monkeypatch.setattr(cli, "download", refuse)
    await _age_runs(30)
    assert await cli._refresh([LINKS_NAME], None, if_stale=True) == 1  # non-zero so a scheduler notices
    async with AsyncSessionLocal() as db:
        assert (await db.execute(text("SELECT count(*) FROM threat_indicators"))).scalar_one() == 3
