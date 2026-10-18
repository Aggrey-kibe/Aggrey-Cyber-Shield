"""Threat-intelligence: canonicalisation, safe download, atomic ingestion, lookup, scan integration."""
import dataclasses
import urllib.request
from datetime import datetime, timedelta, timezone

import pytest
from sqlalchemy import text

from app.db.session import AsyncSessionLocal
from app.services.intel import download as dl
from app.services.intel.feeds import FEEDS
from app.services.intel.ingest import ingest_file
from app.services.intel.lookup import check_urls, source_statuses
from app.services.intel.normalize import canonical_host, canonical_url
from tests.test_scans_api import PASSWORD, signed_in  # noqa: F401

LINKS = dataclasses.replace(FEEDS["phishing_database_links"], min_accepted=3)
DOMAINS = dataclasses.replace(FEEDS["phishing_database_domains"], min_accepted=3)
BAD1 = "http://evil-login.example-phish.xyz/verify/account.php?id=1"
BAD2 = "https://docs.google.com/forms/d/e/1FAIpQLSdemo/viewform"
BAD3 = "http://93.184.216.34/bank/login"        # a public IP (never contacted)


# ------------------------------------------------------------ canonicalisation
def test_scheme_default_port_fragment_case_and_trailing_dot_are_ignored():
    base = canonical_url("http://Evil.Example.com/Path?a=1")
    assert base == canonical_url("https://evil.example.com:443/Path?a=1#frag")
    assert base == canonical_url("evil.example.com./Path?a=1")
    assert base.sha256 != canonical_url("http://evil.example.com/path?a=1").sha256   # path is case-sensitive
    assert base.sha256 != canonical_url("http://evil.example.com/Path?a=2").sha256   # query matters
    assert base.sha256 != canonical_url("http://evil.example.com:8080/Path?a=1").sha256


def test_userinfo_trick_is_looked_up_as_the_real_destination():
    assert canonical_url("https://google.com@evil.example.com/x") == canonical_url("http://evil.example.com/x")


def test_unicode_hosts_become_punycode():
    c = canonical_url("http://" + "раypal.com" + "/login")
    assert c.host.startswith("xn--")


@pytest.mark.parametrize("bad", [
    "javascript:alert(1)", "ftp://example.com/x", "file:///etc/passwd", "http://localhost/", "http://127.0.0.1/",
    "http://10.0.0.5/x", "http://192.168.1.1/", "http://169.254.169.254/latest/meta-data/", "http://0x7f000001/",
    "http://2130706433/", "http://printer.local/", "http://[::1]/", "http://" + "a" * 300 + ".com/", "",
    "http://exa\x00mple.com/", "http://" + "x" * 2100,
])
def test_non_public_or_malformed_urls_are_refused(bad):
    assert canonical_url(bad) is None


def test_public_ip_is_accepted_and_host_helper_rejects_non_hosts():
    assert canonical_url("http://198.51.100.9/a") is None            # documentation range is not a real public address
    assert canonical_url("http://8.8.8.8/a") is not None
    for bad in ("a.com/path", "a.com:80", "u@a.com", "", "-bad-.com", "a..com"):
        assert canonical_host(bad) is None
    assert canonical_host("Sub.Example.COM.").host == "sub.example.com"


# -------------------------------------------------------------- safe download
class _Resp:
    def __init__(self, body=b"x" * 10, ctype="text/plain"):
        self.headers = {"Content-Type": ctype}
        self._chunks = [body[i:i + 4] for i in range(0, len(body), 4)]
    def __enter__(self): return self
    def __exit__(self, *a): return False
    def read(self, n):
        return self._chunks.pop(0) if self._chunks else b""


class _Opener:
    def __init__(self, resp): self.resp = resp
    def open(self, request, timeout=None): return self.resp


def _patch_opener(monkeypatch, resp):
    monkeypatch.setattr(urllib.request, "build_opener", lambda *h: _Opener(resp))


def test_download_allow_list_and_https_only(tmp_path):
    for bad in ("https://evil.example.com/list.txt", "http://raw.githubusercontent.com/x", "file:///etc/passwd",
                "https://raw.githubusercontent.com.evil.com/x"):
        with pytest.raises(dl.DownloadError):
            dl.download(bad, tmp_path / "f", 1000)


def test_download_rejects_oversize_html_and_empty(tmp_path, monkeypatch):
    url = "https://raw.githubusercontent.com/x/y/z.txt"
    _patch_opener(monkeypatch, _Resp(b"x" * 100))
    with pytest.raises(dl.DownloadError, match="size cap"):
        dl.download(url, tmp_path / "a", 50)
    _patch_opener(monkeypatch, _Resp(b"<html>", "text/html; charset=utf-8"))
    with pytest.raises(dl.DownloadError, match="HTML"):
        dl.download(url, tmp_path / "b", 1000)
    _patch_opener(monkeypatch, _Resp(b""))
    with pytest.raises(dl.DownloadError, match="empty"):
        dl.download(url, tmp_path / "c", 1000)
    _patch_opener(monkeypatch, _Resp(b"hello world"))
    size, sha = dl.download(url, tmp_path / "d", 1000)
    assert size == 11 and len(sha) == 64


def test_redirects_are_refused():
    with pytest.raises(dl.DownloadError, match="Redirects"):
        dl._NoRedirect().redirect_request(None, None, 302, "Found", {}, "https://evil.example.com/")


# ------------------------------------------------------------------ ingestion
def _write(tmp_path, lines, name="feed.txt"):
    path = tmp_path / name
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path, path.stat().st_size


async def _ingest(feed, path, size, **kw):
    async with AsyncSessionLocal() as db:
        return await ingest_file(db, feed, path, sha256="a" * 64, size=size, **kw)


async def _count(sql="SELECT count(*) FROM threat_indicators", **p):
    async with AsyncSessionLocal() as db:
        return (await db.execute(text(sql), p)).scalar_one()


@pytest.mark.asyncio
async def test_valid_entries_load_and_invalid_ones_are_rejected_with_reasons(clean_db, tmp_path, monkeypatch):
    # This file is deliberately mostly garbage; relax the ratio guard to see per-line reasons.
    # (The guard itself is tested in test_unhealthy_feed_is_rejected_...)
    monkeypatch.setattr("app.services.intel.ingest.MAX_REJECT_RATIO", 0.99)
    path, size = _write(tmp_path, [
        "# a comment", "", BAD1, BAD2, BAD3 + "x", BAD1,                       # BAD1 duplicated
        "ftp://example.com/x", "javascript:alert(1)", "http://localhost/admin", "http://169.254.169.254/",
        "not a url", "http://" + "a" * 3000 + ".com",
    ])
    run = await _ingest(LINKS, path, size)
    assert run.status == "ok" and run.partial is False
    assert run.accepted == 4 and run.rejected == 6 and run.lines_total == 10
    reasons = run.details["reject_reasons"]
    assert reasons["not_http"] == 3 and reasons["invalid_url"] >= 3
    assert await _count() == 3                                                    # duplicates collapse
    row = await _count("SELECT count(*) FROM threat_indicators WHERE length(value_sha256) = 32")
    assert row == 3                                                              # only 32-byte hashes are stored
    stored_text = await _count("SELECT count(*) FROM threat_indicators WHERE registered_domain LIKE '%/%'")
    assert stored_text == 0


@pytest.mark.asyncio
async def test_reingesting_is_idempotent_and_keeps_first_seen(clean_db, tmp_path):
    path, size = _write(tmp_path, [BAD1, BAD2, BAD3])
    t1 = datetime(2026, 10, 1, tzinfo=timezone.utc)
    t2 = datetime(2026, 10, 3, tzinfo=timezone.utc)
    await _ingest(LINKS, path, size, now=t1)
    run2 = await _ingest(LINKS, path, size, now=t2)
    assert run2.status == "ok" and run2.removed == 0
    assert await _count() == 3
    assert await _count("SELECT count(*) FROM threat_indicators WHERE first_seen_at=:a AND last_seen_at=:b", a=t1, b=t2) == 3


@pytest.mark.asyncio
async def test_full_run_is_a_snapshot_so_cleaned_entries_disappear(clean_db, tmp_path):
    p1, s1 = _write(tmp_path, [BAD1, BAD2, BAD3], "a.txt")
    p2, s2 = _write(tmp_path, [BAD1, BAD2, "http://another-bad.example-phish.top/x"], "b.txt")
    await _ingest(LINKS, p1, s1)
    run = await _ingest(LINKS, p2, s2)
    assert run.status == "ok" and run.removed == 1
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, [BAD3, BAD1])
    assert BAD3 not in res.hits and BAD1 in res.hits


@pytest.mark.asyncio
async def test_partial_runs_never_delete_and_never_count_as_fresh(clean_db, tmp_path):
    path, size = _write(tmp_path, [BAD1, BAD2, BAD3])
    run = await _ingest(LINKS, path, size, limit=2)
    assert run.status == "ok" and run.partial is True and run.accepted == 2
    async with AsyncSessionLocal() as db:
        assert await source_statuses(db) == []
        assert (await check_urls(db, [BAD1])).checked is False


@pytest.mark.asyncio
@pytest.mark.parametrize("name,lines,expect", [
    ("empty", [], "no entries"),
    ("too_few", [BAD1], "only 1 valid"),
    ("mostly_garbage", [BAD1, BAD2, BAD3] + ["garbage"] * 10, "invalid"),
])
async def test_unhealthy_feed_is_rejected_and_database_is_untouched(clean_db, tmp_path, name, lines, expect):
    good, gs = _write(tmp_path, [BAD1, BAD2, BAD3], "good.txt")
    await _ingest(LINKS, good, gs)
    path, size = _write(tmp_path, lines, f"{name}.txt")
    run = await _ingest(LINKS, path, size)
    assert run.status == "rejected" and expect in run.error
    assert await _count() == 3                                                    # previous data intact
    assert await _count("SELECT count(*) FROM threat_indicators WHERE feed_run_id=:r", r=run.id) == 0


@pytest.mark.asyncio
async def test_truncated_and_flooded_feeds_are_rejected(clean_db, tmp_path):
    ten = [f"http://phish{i}.example-phish.xyz/a" for i in range(10)]
    p, s = _write(tmp_path, ten, "ten.txt")
    await _ingest(LINKS, p, s)
    p4, s4 = _write(tmp_path, ten[:4], "four.txt")
    shrunk = await _ingest(LINKS, p4, s4)
    assert shrunk.status == "rejected" and "shrank" in shrunk.error
    flood = [f"http://flood{i}.example-phish.xyz/a" for i in range(40)]
    pf, sf = _write(tmp_path, flood, "flood.txt")
    grown = await _ingest(LINKS, pf, sf)
    assert grown.status == "rejected" and "grew" in grown.error
    assert await _count() == 10


@pytest.mark.asyncio
async def test_unreadable_file_is_recorded_as_failed_and_data_survives(clean_db, tmp_path):
    good, gs = _write(tmp_path, [BAD1, BAD2, BAD3])
    await _ingest(LINKS, good, gs)
    run = await _ingest(LINKS, tmp_path / "missing.txt", 1)
    assert run.status == "failed" and "FileNotFoundError" in run.error
    assert await _count() == 3


@pytest.mark.asyncio
async def test_domain_feed_cannot_poison_brand_or_shared_roots(clean_db, tmp_path, monkeypatch):
    monkeypatch.setattr("app.services.intel.ingest.MAX_REJECT_RATIO", 0.99)   # this feed is mostly poison on purpose
    path, size = _write(tmp_path, [
        "google.com", "login.google.com", "www.paypal.com", "bit.ly", "github.io",   # all refused
        "evil-site.xyz", "abc.github.io", "bad.weeblysite.com", "198.51.100.1x", "a b.com",
    ])
    run = await _ingest(DOMAINS, path, size)
    assert run.status == "ok", run.error                                        # the run really was applied
    reasons = run.details["reject_reasons"]
    assert reasons["protected_brand_domain"] == 3
    assert reasons["protected_shared_root"] == 2
    assert reasons["no_valid_suffix"] == 1 and reasons["invalid_host"] == 1
    assert run.accepted == 3          # evil-site.xyz, abc.github.io, bad.weeblysite.com
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, ["https://www.google.com/", "https://login.google.com/", "https://bit.ly/abc",
                                    "http://evil-site.xyz/anything", "https://abc.github.io/page"])
    assert set(res.hits) == {"http://evil-site.xyz/anything", "https://abc.github.io/page"}


# --------------------------------------------------------------------- lookup
@pytest.mark.asyncio
async def test_url_lookup_is_exact_but_scheme_and_fragment_insensitive(clean_db, tmp_path):
    path, size = _write(tmp_path, [BAD1, BAD2, BAD3])
    await _ingest(LINKS, path, size)
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, [
            BAD1.replace("http://", "https://") + "#x",                           # same address: hit
            "http://evil-login.example-phish.xyz/verify/account.php?id=2",       # other query: no hit
            "http://evil-login.example-phish.xyz/other",                          # other path: no hit
            "https://www.google.com/",                                            # brand site: no hit
            "https://docs.google.com/forms/d/e/OTHER/viewform",                   # other Google page: no hit
            BAD2,
        ])
    assert set(res.hits) == {BAD1.replace("http://", "https://") + "#x", BAD2}
    hit = res.hits[BAD2][0]
    assert hit.source == "phishing_database_links" and hit.license == "MIT" and hit.confidence == "medium"


@pytest.mark.asyncio
async def test_domain_indicator_matches_host_only_not_parent_or_siblings(clean_db, tmp_path):
    path, size = _write(tmp_path, ["bad.example-phish.xyz", "two.example-phish.xyz", "three.example-phish.xyz"])
    await _ingest(DOMAINS, path, size)
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, ["http://bad.example-phish.xyz/any/path?q=1", "http://example-phish.xyz/",
                                    "http://other.example-phish.xyz/", "http://sub.bad.example-phish.xyz/"])
    assert set(res.hits) == {"http://bad.example-phish.xyz/any/path?q=1"}


@pytest.mark.asyncio
async def test_expired_entries_are_ignored_and_staleness_is_reported(clean_db, tmp_path):
    path, size = _write(tmp_path, [BAD1, BAD2, BAD3])
    await _ingest(LINKS, path, size)
    async with AsyncSessionLocal() as db:
        now = datetime.now(timezone.utc)
        fresh = await check_urls(db, [BAD1])
        assert fresh.checked and not fresh.any_stale and BAD1 in fresh.hits
        stale = await check_urls(db, [BAD1], now=now + timedelta(days=5))
        assert stale.any_stale and BAD1 in stale.hits                              # old but still listed
        expired = await check_urls(db, [BAD1], now=now + timedelta(days=30))
        assert BAD1 not in expired.hits                                            # past ttl: ignored


@pytest.mark.asyncio
async def test_ingest_and_lookup_never_touch_the_network(clean_db, tmp_path, monkeypatch):
    def boom(*a, **k):
        raise AssertionError("threat-intel processing must not make web requests")
    monkeypatch.setattr(urllib.request, "urlopen", boom)
    monkeypatch.setattr(urllib.request, "build_opener", boom)
    path, size = _write(tmp_path, [BAD1, BAD2, BAD3])
    await _ingest(LINKS, path, size)
    async with AsyncSessionLocal() as db:
        await check_urls(db, [BAD1, "http://never-contacted.example.org/"])


# ---------------------------------------------------------- scan integration
async def _load(tmp_path, lines=(BAD1, BAD2, BAD3), feed=LINKS):
    path, size = _write(tmp_path, list(lines))
    return await _ingest(feed, path, size)


@pytest.mark.asyncio
async def test_listed_url_becomes_likely_dangerous_with_evidence(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)
    body = (await api.post("/api/v1/scans/url", json={"url": BAD1}, headers=h)).json()
    assert body["verdict"] == "likely_dangerous" and body["risk_score"] >= 85
    assert body["confidence"] == "listed"
    finding = next(f for f in body["findings"] if f["id"] == "known_threat_listed")
    assert "phishing_database_links" in finding["evidence"] and "last seen" in finding["evidence"]
    assert body["intel"]["listed"] is True and body["intel"]["checked"] is True
    src = body["intel"]["sources"][0]
    assert src["fresh"] is True and src["license"] == "MIT" and src["indicators"] == 3
    assert body["intel"]["hits"][0]["confidence"] == "medium"


@pytest.mark.asyncio
async def test_unlisted_url_with_fresh_feeds_says_so_honestly(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)
    body = (await api.post("/api/v1/scans/url", json={"url": "https://www.google.com/search?q=x"}, headers=h)).json()
    assert body["verdict"] == "no_warning_signs" and body["confidence"] == "limited"
    assert body["intel"]["checked"] is True and body["intel"]["listed"] is False
    assert any("not being listed means little" in x for x in body["limitations"])


@pytest.mark.asyncio
async def test_without_loaded_feeds_the_result_says_threat_lists_were_not_checked(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    body = (await api.post("/api/v1/scans/url", json={"url": BAD1}, headers=h)).json()
    assert body["intel"]["checked"] is False
    assert any("No threat lists are loaded" in x for x in body["limitations"])
    assert body["verdict"] != "likely_dangerous" or body["risk_score"] < 85       # no listing evidence was invented


@pytest.mark.asyncio
async def test_stale_feeds_are_disclosed(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)
    async with AsyncSessionLocal() as db:
        await db.execute(text("UPDATE threat_feed_runs SET finished_at = now() - interval '5 days'"))
        await db.commit()
    body = (await api.post("/api/v1/scans/url", json={"url": BAD1}, headers=h)).json()
    assert body["intel"]["sources"][0]["fresh"] is False
    assert any("out of date" in x for x in body["limitations"])
    assert body["intel"]["listed"] is True                                         # stale data still warns


@pytest.mark.asyncio
async def test_message_with_listed_link_is_flagged_per_link_and_overall(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)
    msg = f"Hi, please see {BAD1} thanks"
    body = (await api.post("/api/v1/scans/message", json={"message": msg}, headers=h)).json()
    assert body["risk_score"] >= 85 and body["confidence"] == "listed"
    assert body["urls"][0]["listed_in_threat_feed"] is True
    assert any(f["id"] == "known_threat_listed" for f in body["urls"][0]["findings"])
    assert any(f["id"] == "known_threat_listed" for f in body["findings"])

    plain = (await api.post("/api/v1/scans/message", json={"message": "see you at 5pm"}, headers=h)).json()
    assert plain["intel"] is None                                                  # no link, nothing to look up


@pytest.mark.asyncio
async def test_specific_google_url_can_be_listed_without_condemning_google(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)
    listed = (await api.post("/api/v1/scans/url", json={"url": BAD2}, headers=h)).json()
    other = (await api.post("/api/v1/scans/url", json={"url": "https://docs.google.com/document/d/abc/edit"}, headers=h)).json()
    assert listed["intel"]["listed"] is True and other["intel"]["listed"] is False
    assert other["verdict"] == "no_warning_signs"


@pytest.mark.asyncio
async def test_domain_feed_hit_marks_any_page_on_that_host(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path, ["bad.example-phish.xyz", "x1.example-phish.xyz", "x2.example-phish.xyz"], DOMAINS)
    body = (await api.post("/api/v1/scans/url", json={"url": "http://bad.example-phish.xyz/new/path"}, headers=h)).json()
    assert body["intel"]["listed"] is True and body["intel"]["hits"][0]["indicator_type"] == "domain"


@pytest.mark.asyncio
async def test_intel_status_endpoint_needs_sign_in_and_reports_freshness(api, sent_emails, tmp_path):
    assert (await api.get("/api/v1/intel/status")).status_code == 401
    h = await signed_in(api, sent_emails, "a@example.com")
    empty = (await api.get("/api/v1/intel/status", headers=h)).json()
    assert empty == {"checked": False, "sources": []}
    await _load(tmp_path)
    data = (await api.get("/api/v1/intel/status", headers=h)).json()
    assert data["checked"] is True and data["sources"][0]["name"] == "phishing_database_links"
    assert not any(k in str(data) for k in ("http://", "https://"))          # status never exposes indicator text


@pytest.mark.asyncio
async def test_old_scans_without_intel_block_still_load(api, sent_emails):
    h = await signed_in(api, sent_emails, "a@example.com")
    sid = (await api.post("/api/v1/scans/url", json={"url": "https://www.google.com/"}, headers=h)).json()["id"]
    async with AsyncSessionLocal() as db:
        await db.execute(text("UPDATE scans SET result = result::jsonb - 'intel'"))
        await db.commit()
    got = await api.get(f"/api/v1/scans/{sid}", headers=h)
    assert got.status_code == 200 and got.json()["intel"] is None


# ------------------------------------------- reviewed protections (found by auditing the real feed)
@pytest.mark.asyncio
async def test_reviewed_legitimate_and_platform_roots_are_protected(clean_db, tmp_path, monkeypatch):
    monkeypatch.setattr("app.services.intel.ingest.MAX_REJECT_RATIO", 0.99)
    path, size = _write(tmp_path, [
        "gravatar.com", "x.getpocket.com", "nflxvideo.net",                   # legitimate services: nothing under them
        "pastebin.com", "unbouncepages.com",                                  # platform roots: refused
        "tenant-bad.unbouncepages.com", "evil-site.xyz", "other-bad.example-phish.top",   # specific tenants / own domains: accepted
    ])
    run = await _ingest(DOMAINS, path, size)
    assert run.status == "ok", run.error
    assert run.details["reject_reasons"]["protected_brand_domain"] == 3
    assert run.details["reject_reasons"]["protected_shared_root"] == 2
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, ["https://gravatar.com/avatar/1", "https://pastebin.com/raw/abc",
                                    "https://tenant-bad.unbouncepages.com/lp", "https://other-tenant.unbouncepages.com/lp",
                                    "http://evil-site.xyz/"])
    assert set(res.hits) == {"https://tenant-bad.unbouncepages.com/lp", "http://evil-site.xyz/"}


@pytest.mark.asyncio
async def test_exact_pastebin_page_can_still_be_listed_without_condemning_pastebin(clean_db, tmp_path):
    bad_paste = "https://pastebin.com/raw/PHISHKIT1"
    path, size = _write(tmp_path, [bad_paste, BAD1, BAD3])
    await _ingest(LINKS, path, size)
    async with AsyncSessionLocal() as db:
        res = await check_urls(db, [bad_paste, "https://pastebin.com/raw/SOMETHINGELSE", "https://pastebin.com/"])
    assert set(res.hits) == {bad_paste}


@pytest.mark.asyncio
async def test_whole_host_listing_is_weighed_below_an_exact_page_listing(api, sent_emails, tmp_path):
    h = await signed_in(api, sent_emails, "a@example.com")
    await _load(tmp_path)                                                                    # exact URLs
    await _load(tmp_path, ["whole-host.example-phish.xyz", "w2.example-phish.top", "w3.example-phish.top"], DOMAINS)
    exact = (await api.post("/api/v1/scans/url", json={"url": BAD1}, headers=h)).json()
    broad = (await api.post("/api/v1/scans/url", json={"url": "http://whole-host.example-phish.xyz/x"}, headers=h)).json()
    assert exact["verdict"] == broad["verdict"] == "likely_dangerous"
    assert exact["risk_score"] >= 85
    exact_f = next(f for f in exact["findings"] if f["id"] == "known_threat_listed")
    broad_f = next(f for f in broad["findings"] if f["id"] == "known_threat_listed")
    assert "exact page" in exact_f["explanation"] and "whole site" in broad_f["explanation"]
    assert "mistakes" in broad_f["explanation"]
