"""
Download the public evaluation feeds (the LIST FILES only).

This script never contacts any phishing URL. It fetches two text files over
HTTPS from an allow-listed host, with a timeout and a size cap, and records a
manifest (source URL, retrieval time, size, SHA-256, license) so results can be
reproduced and audited. Downloaded data is git-ignored and never redistributed.

    python -m evaluation.fetch_external
"""
import hashlib
import json
import sys
import urllib.request
from datetime import datetime, timezone
from urllib.parse import urlsplit

from evaluation.datasets import EXTERNAL_DIR

ALLOWED_HOSTS = {"raw.githubusercontent.com"}
MAX_BYTES = 120 * 1024 * 1024
TIMEOUT_SECONDS = 120

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
    parts = urlsplit(url)
    if parts.scheme != "https" or parts.hostname not in ALLOWED_HOSTS:
        raise ValueError(f"Refusing to fetch from non-allow-listed location: {url}")
    sha = hashlib.sha256()
    size = 0
    request = urllib.request.Request(url, headers={"User-Agent": "aggrey-shield-eval/1.0"})
    with urllib.request.urlopen(request, timeout=TIMEOUT_SECONDS) as response, open(dest, "wb") as out:
        while chunk := response.read(1024 * 256):
            size += len(chunk)
            if size > MAX_BYTES:
                raise ValueError("Download exceeds the size cap; aborting.")
            sha.update(chunk)
            out.write(chunk)
    return size, sha.hexdigest()


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
