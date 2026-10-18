"""Dataset loading, deterministic splitting and defanging. No network access."""
import hashlib
import ipaddress
import json
import random
from dataclasses import dataclass
from pathlib import Path

import tldextract

DATA_DIR = Path(__file__).parent / "data"
EXTERNAL_DIR = DATA_DIR / "external"
_EXTRACT = tldextract.TLDExtract(suffix_list_urls=(), cache_dir=None)

# Independent of the detector's own lists on purpose (see DATASETS.md).
SHARED_PLATFORMS = frozenset({
    "google.com", "weebly.com", "wixsite.com", "github.io", "blogspot.com",
    "web.app", "firebaseapp.com", "herokuapp.com", "netlify.app", "vercel.app",
    "pages.dev", "workers.dev", "000webhostapp.com", "sharepoint.com",
    "dropbox.com", "onrender.com", "glitch.me", "ipfs.io", "notion.site",
    "canva.com", "typeform.com", "forms.gle", "wordpress.com", "medium.com",
    "squarespace.com", "godaddysites.com", "yolasite.com", "webflow.io",
    "carrd.co", "linktr.ee", "bit.ly", "tinyurl.com", "ow.ly", "cutt.ly",
})


@dataclass(frozen=True)
class Sample:
    id: str
    kind: str          # "url" | "message"
    text: str
    label: str         # "malicious" | "benign"
    category: str
    source: str        # provenance: where this sample came from
    license: str

    @property
    def malicious(self) -> bool:
        return self.label == "malicious"


def load_jsonl(path: Path) -> list[Sample]:
    samples = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.startswith("#"):
            samples.append(Sample(**json.loads(line)))
    return samples


def defang(text: str) -> str:
    """Make a URL unclickable for reports: hxxp://example[.]com"""
    return (
        text.replace("http", "hxxp").replace("HTTP", "HXXP").replace(".", "[.]")
    )[:140]


def registered_domain(url: str) -> str:
    host = url.split("://", 1)[-1].split("/", 1)[0].split("@")[-1].split(":")[0]
    return (_EXTRACT(host).top_domain_under_public_suffix or host).lower()


def url_category(url: str) -> str:
    """Category from the URL string alone (does not use detector output)."""
    host = url.split("://", 1)[-1].split("/", 1)[0].split("@")[-1].split(":")[0].strip("[]")
    try:
        ipaddress.ip_address(host)
        return "ip_host"
    except ValueError:
        pass
    return "shared_platform" if registered_domain(url) in SHARED_PLATFORMS else "own_domain"


def split_of(key: str, salt: str = "aggrey-eval-v1") -> str:
    """Deterministic 50/50 dev/test split keyed by REGISTERED DOMAIN (no leakage)."""
    digest = hashlib.sha256(f"{salt}:{key}".encode()).digest()
    return "dev" if digest[0] % 2 == 0 else "test"


def build_external(seed: int = 1337, n_pos: int = 2000, n_neg: int = 2000,
                   max_per_domain: int = 2) -> list[Sample]:
    """Sample the downloaded public feeds deterministically."""
    phish_file = EXTERNAL_DIR / "phishing-links-ACTIVE.txt"
    benign_file = EXTERNAL_DIR / "opendns-top-domains.txt"
    if not phish_file.exists() or not benign_file.exists():
        raise FileNotFoundError(
            "External feeds not downloaded. Run: python -m evaluation.fetch_external"
        )
    rng = random.Random(seed)

    lines = [
        ln.strip() for ln in phish_file.read_text(encoding="utf-8", errors="replace").splitlines()
    ]
    lines = [ln for ln in lines if ln.startswith(("http://", "https://")) and " " not in ln and len(ln) <= 2048]
    rng.shuffle(lines)
    per_domain: dict[str, int] = {}
    positives: list[Sample] = []
    for url in lines:
        dom = registered_domain(url)
        if per_domain.get(dom, 0) >= max_per_domain:
            continue
        per_domain[dom] = per_domain.get(dom, 0) + 1
        positives.append(Sample(
            id=f"pdb-{hashlib.sha1(url.encode()).hexdigest()[:12]}", kind="url", text=url,
            label="malicious", category=url_category(url),
            source="Phishing.Database phishing-links-ACTIVE.txt", license="MIT",
        ))
        if len(positives) >= n_pos:
            break

    domains = [d.strip() for d in benign_file.read_text(encoding="utf-8").splitlines() if d.strip()]
    rng.shuffle(domains)
    negatives = [
        Sample(id=f"odns-{i}", kind="url", text=f"https://{d}/", label="benign",
               category=url_category(f"https://{d}/"),
               source="OpenDNS public-domain-lists top domains", license="unverified")
        for i, d in enumerate(domains[:n_neg])
    ]
    return positives + negatives
