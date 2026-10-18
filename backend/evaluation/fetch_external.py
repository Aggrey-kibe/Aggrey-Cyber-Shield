"""
Download the public evaluation feeds (the LIST FILES only).

This script never contacts any phishing URL. It fetches two text files over
HTTPS from an allow-listed host, with a timeout and a size cap, and records a
manifest (source URL, retrieval time, size, SHA-256, license) so results can be
reproduced and audited. Downloaded data is git-ignored and never redistributed.

    python -m evaluation.fetch_external
"""
import json
import sys
from datetime import datetime, timezone

from evaluation.datasets import EXTERNAL_DIR

MAX_BYTES = 120 * 1024 * 1024

FEEDS = [
    {
        "name": "phishing-links-ACTIVE.txt",
        "url": "https://raw.githubusercontent.com/mitchellkrogza/Phishing.Database/master/phishing-links-ACTIVE.txt",
        "license": "MIT (Copyright Mitchell Krog and Phishing.Database contributors)",
        "role": "malicious (phishing links; aggregated from third-party feeds, labels are noisy)",
    },
    {
        "name": "opendns-top-domains.txt",
        "url": "https://raw.githubusercontent.com/opendns/public-domain-lists/master/opendns-top-domains.txt",
        "license": "UNVERIFIED (no LICENSE file found); used locally for evaluation only, never redistributed",
        "role": "benign (popular domains)",
    },
]


def _download(url: str, dest) -> tuple[int, str]:
    """Single hardened implementation shared with the threat-intel pipeline."""
    from app.services.intel.download import download
    return download(url, dest, MAX_BYTES)


def main() -> int:
    EXTERNAL_DIR.mkdir(parents=True, exist_ok=True)
    manifest = {"retrieved_at_utc": datetime.now(timezone.utc).isoformat(), "feeds": []}
    for feed in FEEDS:
        dest = EXTERNAL_DIR / feed["name"]
        size, digest = _download(feed["url"], dest)
        if size == 0:
            print(f"ERROR: {feed['name']} downloaded empty", file=sys.stderr)
            return 1
        manifest["feeds"].append({**feed, "bytes": size, "sha256": digest})
        print(f"{feed['name']}: {size:,} bytes sha256={digest[:16]}...")
    (EXTERNAL_DIR / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
