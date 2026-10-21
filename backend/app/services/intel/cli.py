"""
Command line for threat-feed maintenance (run from the backend folder):

    python -m app.services.intel.cli refresh --all
    python -m app.services.intel.cli refresh --all --if-stale     # for schedulers: does real work only when a list is old
    python -m app.services.intel.cli refresh --source phishing_database_domains
    python -m app.services.intel.cli refresh --source phishing_database_links --limit 5000   # dev sample, not counted as fresh
    python -m app.services.intel.cli status

Output never contains feed URLs or indicator text.
"""
import argparse
import asyncio
import sys
import tempfile
from pathlib import Path

from sqlalchemy import select, text

from app.db.session import AsyncSessionLocal, engine
from app.models import ThreatFeedRun
from app.services.intel.download import DownloadError, download
from app.services.intel.feeds import FEEDS
from app.services.intel.ingest import ingest_file
from app.services.intel.lookup import source_statuses


# Arbitrary constant naming this job in PostgreSQL's advisory-lock table: only one refresh may run at a time,
# even across several containers or a manual run overlapping a scheduled one.
LOCK_KEY = 7_204_118_903


async def _refresh(names: list[str], limit: int | None, if_stale: bool = False) -> int:
    async with engine.connect() as lock_conn:
        got = (await lock_conn.execute(text("SELECT pg_try_advisory_lock(:k)"), {"k": LOCK_KEY})).scalar_one()
        await lock_conn.commit()              # the session-level lock outlives the transaction
        if not got:
            print("another refresh is already running; nothing to do")
            return 0
        try:
            return await _refresh_locked(names, limit, if_stale)
        finally:
            await lock_conn.execute(text("SELECT pg_advisory_unlock(:k)"), {"k": LOCK_KEY})
            await lock_conn.commit()


async def _refresh_locked(names: list[str], limit: int | None, if_stale: bool) -> int:
    failures = 0
    ages: dict[str, float] = {}
    if if_stale:
        async with AsyncSessionLocal() as db:
            ages = {s.name: s.age_hours for s in await source_statuses(db)}
    for name in names:
        feed = FEEDS[name]
        if if_stale and name in ages and ages[name] < feed.refresh_after_hours:
            print(f"[{name}] fresh ({ages[name]:.1f}h old, refreshes after {feed.refresh_after_hours}h): skipped")
            continue
        tmp = Path(tempfile.mkstemp(prefix="intel-", suffix=".txt")[1])
        try:
            print(f"[{name}] downloading list file ...", flush=True)
            size, sha = await asyncio.to_thread(download, feed.url, tmp, feed.max_bytes)
            print(f"[{name}] {size:,} bytes, sha256 {sha[:16]}...; validating and loading ...", flush=True)
            async with AsyncSessionLocal() as db:
                run = await ingest_file(db, feed, tmp, sha256=sha, size=size, limit=limit)
            print(f"[{name}] status={run.status} lines={run.lines_total:,} accepted={run.accepted:,} "
                  f"rejected={run.rejected:,} removed={run.removed:,} reasons={run.details.get('reject_reasons')}")
            if run.status != "ok":
                failures += 1
                print(f"[{name}] NOT APPLIED: {run.error}")
        except DownloadError as exc:
            failures += 1
            print(f"[{name}] download refused/failed: {exc}")
        finally:
            tmp.unlink(missing_ok=True)
    return 1 if failures else 0


async def _status() -> int:
    async with AsyncSessionLocal() as db:
        statuses = await source_statuses(db)
        if not statuses:
            print("No fully-loaded feeds yet. Run: python -m app.services.intel.cli refresh --all")
        for s in statuses:
            print(f"{s.name}: {s.indicators:,} indicators, refreshed {s.age_hours:.1f}h ago "
                  f"({'fresh' if s.fresh else 'STALE'}), licence {s.license}")
        runs = (await db.execute(select(ThreatFeedRun).order_by(ThreatFeedRun.started_at.desc()).limit(8))).scalars().all()
        print("\nrecent runs:")
        for r in runs:
            print(f"  {r.started_at:%Y-%m-%d %H:%M} {r.source:28s} {r.status:9s} accepted={r.accepted:,}"
                  f"{' partial' if r.partial else ''}{'  ' + r.error if r.error else ''}")
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(prog="intel")
    sub = parser.add_subparsers(dest="command", required=True)
    refresh = sub.add_parser("refresh")
    refresh.add_argument("--source", choices=sorted(FEEDS))
    refresh.add_argument("--all", action="store_true")
    refresh.add_argument("--limit", type=int, help="load only the first N lines (development; not counted as fresh)")
    refresh.add_argument("--if-stale", action="store_true",
                         help="only reload a feed that is older than its refresh interval (safe to schedule hourly)")
    sub.add_parser("status")
    args = parser.parse_args(argv)

    async def run() -> int:
        try:
            if args.command == "status":
                return await _status()
            if not args.all and not args.source:
                parser.error("choose --all or --source NAME")
            return await _refresh(sorted(FEEDS) if args.all else [args.source], args.limit, args.if_stale)
        finally:
            await engine.dispose()

    return asyncio.run(run())


if __name__ == "__main__":
    sys.exit(main())
