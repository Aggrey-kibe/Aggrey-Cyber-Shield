"""
Command line for threat-feed maintenance (run from the backend folder):

    python -m app.services.intel.cli refresh --all
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

from sqlalchemy import select

from app.db.session import AsyncSessionLocal, engine
from app.models import ThreatFeedRun
from app.services.intel.download import DownloadError, download
from app.services.intel.feeds import FEEDS
from app.services.intel.ingest import ingest_file
from app.services.intel.lookup import source_statuses


async def _refresh(names: list[str], limit: int | None) -> int:
    failures = 0
    for name in names:
        feed = FEEDS[name]
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
    sub.add_parser("status")
    args = parser.parse_args(argv)

    async def run() -> int:
        try:
            if args.command == "status":
                return await _status()
            if not args.all and not args.source:
                parser.error("choose --all or --source NAME")
            return await _refresh(sorted(FEEDS) if args.all else [args.source], args.limit)
        finally:
            await engine.dispose()

    return asyncio.run(run())


if __name__ == "__main__":
    sys.exit(main())
