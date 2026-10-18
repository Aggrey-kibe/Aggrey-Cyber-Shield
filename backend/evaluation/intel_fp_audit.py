"""
Audit the LOADED threat feeds against a popularity list to find false-positive risk.

A feed that lists a popular site as phishing will show users a red warning on a
legitimate site. Run this after every refresh and review each line by hand:
exempt only domains you have confirmed are legitimate (see app/services/intel/feeds.py).

    python -m app.services.intel.cli refresh --all
    python -m evaluation.fetch_external            # downloads the popularity list
    python -m evaluation.intel_fp_audit [--top 10000]

Results are measured on POPULAR sites only; small legitimate sites are not covered.
"""
import argparse
import asyncio

from app.db.session import AsyncSessionLocal, engine
from app.services.intel.feeds import REVIEWED_LEGITIMATE, REVIEWED_PLATFORMS
from app.services.intel.lookup import check_urls
from evaluation.datasets import EXTERNAL_DIR, defang


async def audit(top: int) -> list[tuple[int, str, str]]:
    path = EXTERNAL_DIR / "opendns-top-domains.txt"
    if not path.exists():
        raise SystemExit("Popularity list missing. Run: python -m evaluation.fetch_external")
    domains = [d.strip() for d in path.read_text(encoding="utf-8").splitlines() if d.strip()][:top]
    found = []
    async with AsyncSessionLocal() as db:
        for i in range(0, len(domains), 100):
            result = await check_urls(db, [f"https://{d}/" for d in domains[i:i + 100]])
            for url, hits in result.hits.items():
                found.append((domains.index(url.split("//")[1].rstrip("/")) + 1, url, ",".join(sorted({h.indicator_type for h in hits}))))
    await engine.dispose()
    return sorted(found)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--top", type=int, default=10000)
    args = parser.parse_args()
    found = asyncio.run(audit(args.top))
    print(f"{len(found)} of the top {args.top} popular domains are flagged by the loaded feeds")
    for rank, url, kinds in found:
        host = url.split("//")[1].rstrip("/")
        status = "reviewed-exempt?!" if host in REVIEWED_LEGITIMATE | REVIEWED_PLATFORMS else "NEEDS REVIEW"
        print(f"  rank {rank:5d}  {defang(url):40s} match={kinds:8s} {status}")
    return 1 if found else 0


if __name__ == "__main__":
    raise SystemExit(main())
