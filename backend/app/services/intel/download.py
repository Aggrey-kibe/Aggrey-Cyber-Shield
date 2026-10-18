"""
Safe download of feed list files.

Only HTTPS, only allow-listed hosts, redirects refused (a redirect could leave
the allow-list), hard size cap, timeout, and HTML error pages rejected. This
fetches THE LIST FILE; it never contacts any URL that appears inside it.
"""
import hashlib
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urlsplit

ALLOWED_HOSTS = frozenset({"raw.githubusercontent.com"})
TIMEOUT_SECONDS = 120


class DownloadError(ValueError):
    pass


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):  # noqa: D401
        raise DownloadError("Redirects are refused (they could leave the allow-list).")


def download(url: str, dest: Path, max_bytes: int) -> tuple[int, str]:
    """Download `url` to `dest`. Returns (bytes, sha256)."""
    parts = urlsplit(url)
    if parts.scheme != "https" or parts.hostname not in ALLOWED_HOSTS:
        raise DownloadError(f"Refusing to fetch from non-allow-listed location: {url}")
    opener = urllib.request.build_opener(_NoRedirect)
    request = urllib.request.Request(url, headers={"User-Agent": "aggrey-shield-intel/1.0"})
    sha = hashlib.sha256()
    size = 0
    try:
        with opener.open(request, timeout=TIMEOUT_SECONDS) as response, open(dest, "wb") as out:
            if "html" in (response.headers.get("Content-Type") or "").lower():
                raise DownloadError("Server returned an HTML page, not a list file.")
            while chunk := response.read(256 * 1024):
                size += len(chunk)
                if size > max_bytes:
                    raise DownloadError("Download exceeds the size cap; aborting.")
                sha.update(chunk)
                out.write(chunk)
    except urllib.error.URLError as exc:
        raise DownloadError(f"Download failed: {exc.reason}") from exc
    if size == 0:
        raise DownloadError("Downloaded file is empty.")
    return size, sha.hexdigest()
