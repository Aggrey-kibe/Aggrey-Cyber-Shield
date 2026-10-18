"""
Explainable, offline URL analysis.

analyze_url() takes untrusted text and returns a score from 0 to 100 plus a
list of findings, each saying in plain words WHY it matters. It never makes a
network request.
"""
import ipaddress
import re
import unicodedata
from urllib.parse import parse_qsl, urlsplit

import tldextract

from app.services.scanning.brands import (
    ARCHIVE_EXTENSIONS,
    BRANDS,
    DANGEROUS_SCHEMES,
    FREE_HOSTING,
    REDIRECT_PARAMS,
    RISKY_FILE_EXTENSIONS,
    SENSITIVE_WORDS,
    SUSPICIOUS_TLDS,
    URL_SHORTENERS,
    Brand,
)
from app.services.scanning.types import Finding, UrlAnalysis, score_from

MAX_URL_LENGTH = 2048

# Offline: uses the public-suffix snapshot bundled with the library.
_EXTRACT = tldextract.TLDExtract(suffix_list_urls=(), cache_dir=None)

_SCHEME_RE = re.compile(r"^([a-zA-Z][a-zA-Z0-9+.\-]*):")
_NUMERIC_PART_RE = re.compile(r"^(0x[0-9a-f]+|\d+)$", re.IGNORECASE)
_TOKEN_SPLIT_RE = re.compile(r"[^a-z0-9]+")

_CONFUSABLES = {
    # Cyrillic / Greek letters that look like Latin ones
    "а": "a", "е": "e", "о": "o", "р": "p", "с": "c", "у": "y", "х": "x",
    "і": "i", "ј": "j", "ѕ": "s", "ӏ": "l", "ɡ": "g", "ο": "o", "α": "a",
    "ν": "v", "ρ": "p", "ι": "i", "τ": "t", "κ": "k", "υ": "u",
    # digits / symbols used in place of letters
    "0": "o", "1": "l", "5": "s", "3": "e", "4": "a", "7": "t",
    "$": "s", "@": "a",
}


class InvalidUrl(ValueError):
    """The text cannot be analysed as a URL (a client error, not a threat)."""


def _skeleton(text: str) -> str:
    """Reduce look-alike characters so 'раураl' and 'paypa1' become 'paypal'."""
    text = unicodedata.normalize("NFKD", text.lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = "".join(_CONFUSABLES.get(c, c) for c in text)
    text = re.sub(r"[^a-z0-9]", "", text)
    return text.replace("rn", "m").replace("vv", "w")


def _distance_leq_one(a: str, b: str) -> bool:
    """True when a and b differ by at most one insert/delete/substitute."""
    if a == b:
        return True
    if abs(len(a) - len(b)) > 1:
        return False
    if len(a) == len(b):
        return sum(x != y for x, y in zip(a, b)) == 1
    shorter, longer = sorted((a, b), key=len)
    i = j = edits = 0
    while i < len(shorter) and j < len(longer):
        if shorter[i] == longer[j]:
            i += 1
            j += 1
        else:
            edits += 1
            j += 1
            if edits > 1:
                return False
    return True


def _tokens(text: str) -> list[str]:
    return [t for t in _TOKEN_SPLIT_RE.split(text.lower()) if t]


def _brand_match(text: str, brand: Brand) -> str | None:
    """
    How does `text` use the brand's name?

    "exact":  as a whole word (or look-alike spelling of it), including names
              split by hyphens, e.g. 'm-pesa' or 'paypa1'.
    "prefix": only as the start of a longer word, e.g. 'appleinsider'. Much
              weaker evidence (many genuine sites do this).
    """
    needle = brand.label
    tokens = _tokens(text)
    skeletons = [_skeleton(t) for t in tokens]
    for i in range(len(tokens)):
        joined = ""
        for k in range(i, min(i + 3, len(tokens))):
            joined += skeletons[k]
            if joined == needle:
                return "exact"
    if len(needle) >= 5 and any(
        sk.startswith(needle) and sk != needle for sk in skeletons
    ):
        return "prefix"
    return None


def _is_official(brand: Brand, registered: str, domain_label: str, suffix: str) -> bool:
    if registered in brand.official:
        return True
    if brand.allow_country_domains and domain_label == brand.label:
        last = suffix.rsplit(".", 1)[-1] if suffix else ""
        return len(last) == 2  # a country-code ending such as .ke, .uk
    return False


def _scripts(label: str) -> set[str]:
    scripts = set()
    for ch in label:
        if ch.isalpha():
            if ord(ch) < 128:
                scripts.add("LATIN")
            else:
                scripts.add(unicodedata.name(ch, "UNKNOWN").split(" ")[0])
    return scripts


def _decode_host(host: str) -> tuple[str, bool, bool]:
    """Return (unicode_host, has_idn, malformed_punycode)."""
    decoded_labels = []
    has_idn = any(ord(c) > 127 for c in host)
    malformed = False
    for label in host.split("."):
        if label.startswith("xn--"):
            has_idn = True
            try:
                label = label[4:].encode("ascii").decode("punycode")
            except (UnicodeError, ValueError):
                malformed = True
        decoded_labels.append(label)
    return ".".join(decoded_labels), has_idn, malformed


def _clean(raw: str) -> str:
    if not isinstance(raw, str):
        raise InvalidUrl("A URL must be text.")
    text = raw.strip()
    if not text:
        raise InvalidUrl("Please enter a link to check.")
    if len(text) > MAX_URL_LENGTH:
        raise InvalidUrl(f"That link is too long (maximum {MAX_URL_LENGTH} characters).")
    if any(ord(c) < 32 or ord(c) == 127 for c in text):
        raise InvalidUrl("The link contains invisible control characters.")
    return text


def analyze_url(raw: str) -> UrlAnalysis:
    text = _clean(raw)
    findings: list[Finding] = []

    def add(fid, severity, title, explanation, weight, evidence=None):
        findings.append(Finding(fid, severity, title, explanation, weight, evidence))

    # --- scheme -----------------------------------------------------------
    explicit_scheme = None
    match = _SCHEME_RE.match(text)
    if match and (text[match.end():].startswith("//") or match.group(1).lower() in DANGEROUS_SCHEMES):
        explicit_scheme = match.group(1).lower()

    if explicit_scheme in DANGEROUS_SCHEMES:
        add(
            "dangerous_scheme", "high", "Not a normal web link",
            f"A '{explicit_scheme}:' link does not open a website. It can run "
            "code or open local content, and is a known way to attack people.",
            60, explicit_scheme + ":",
        )
        return UrlAnalysis(text, text[:200], None, None, score_from(findings), findings)

    if explicit_scheme in ("http", "https"):
        to_parse = text
    elif explicit_scheme is not None:
        to_parse = text
        add(
            "unusual_scheme", "medium", "Unusual link type",
            f"This link uses '{explicit_scheme}://' instead of a normal web "
            "address (http or https).", 15, explicit_scheme + "://",
        )
    else:
        to_parse = "http://" + text.lstrip("/")

    try:
        parts = urlsplit(to_parse)
        host = parts.hostname
    except ValueError as exc:
        raise InvalidUrl("That does not look like a valid web address.") from exc
    if not host:
        raise InvalidUrl("No website address (host name) was found in that link.")
    host = host.rstrip(".").lower()
    if not host:
        raise InvalidUrl("No website address (host name) was found in that link.")

    netloc = parts.netloc
    normalized = (
        f"{'https' if explicit_scheme == 'https' else 'http' if explicit_scheme == 'http' else 'http'}://"
        f"{host}{parts.path or '/'}"
    )
    if parts.query:
        normalized += "?" + parts.query
    normalized = normalized[:MAX_URL_LENGTH]

    # --- credentials trick: https://google.com@evil.com -------------------
    if "@" in netloc:
        shown = netloc.rsplit("@", 1)[0]
        add(
            "userinfo_trick", "high", "Hidden destination before an '@' sign",
            "Everything before the '@' is ignored by the browser. Scammers put "
            "a trusted name there so the link looks safe while it really goes "
            f"to '{host}'.", 30, shown[:80],
        )

    # --- port ------------------------------------------------------------
    try:
        port = parts.port
    except ValueError:
        port = -1
    if port == -1:
        add("invalid_port", "medium", "Invalid port number",
            "The link has a malformed port number, which normal links do not.", 10)
    elif port not in (None, 80, 443):
        add("odd_port", "medium", "Unusual port number",
            "Normal websites use the default ports. A custom port is common on "
            "temporary or hidden servers.", 10, f":{port}")

    # --- raw IP address --------------------------------------------------
    host_for_ip = host.strip("[]")
    ip_like = False
    obfuscated_ip = False
    try:
        ipaddress.ip_address(host_for_ip)
        ip_like = True
    except ValueError:
        parts_h = host.split(".")
        if all(_NUMERIC_PART_RE.match(p) for p in parts_h):
            ip_like = True
            obfuscated_ip = True
    if ip_like:
        if obfuscated_ip:
            add("obfuscated_ip", "high", "Disguised numeric address",
                "The address is written as a hidden number form that browsers "
                "still understand. This is used to hide where a link goes.",
                35, host)
        else:
            add("ip_address", "high", "Link points to a number, not a name",
                "Real companies use names like 'bank.co.ke'. A raw number "
                "address is typical of throw-away scam servers.", 25, host)
        return UrlAnalysis(text, normalized, host, None, score_from(findings), findings)

    # --- internationalised / look-alike characters -----------------------
    unicode_host, has_idn, malformed = _decode_host(host)
    if malformed:
        add("bad_punycode", "high", "Corrupted international address",
            "Part of the address is in a broken 'xn--' encoding.", 20)
    if has_idn:
        add("idn_host", "medium", "Address uses non-English characters",
            "Addresses with accented or foreign letters can imitate well-known "
            "names (for example, a Cyrillic 'а' looks like a Latin 'a').", 15,
            unicode_host[:80])
        for label in unicode_host.split("."):
            if len(_scripts(label)) > 1:
                add("mixed_scripts", "high", "Mixes different alphabets",
                    "One part of the address mixes letters from different "
                    "alphabets (such as Latin and Cyrillic). That is a classic "
                    "look-alike trick.", 35, label[:60])
                break

    # --- domain structure -------------------------------------------------
    ascii_host = host
    ext = _EXTRACT(ascii_host)
    registered = ext.top_domain_under_public_suffix or None
    subdomain = ext.subdomain
    domain_label = ext.domain
    suffix = ext.suffix

    if not registered:
        add("no_known_ending", "low", "Unrecognised address ending",
            "The address does not end in a recognised internet domain ending.",
            8, host[:80])

    sub_labels = [s for s in subdomain.split(".") if s] if subdomain else []
    if len(sub_labels) >= 4:
        add("many_subdomains", "medium", "Very long chain of sub-names",
            "Scammers pile up sub-names so the real site name is pushed out of "
            "view.", 10, host[:100])

    if domain_label.removeprefix("xn--").count("-") >= 3:
        add("many_hyphens", "low", "Many hyphens in the site name",
            "Fake sites often use long hyphenated names like "
            "'secure-login-update-account'.", 8, domain_label)

    last_label = suffix.rsplit(".", 1)[-1] if suffix else ""
    if last_label in SUSPICIOUS_TLDS:
        add("risky_ending", "low", f"Uses the '.{last_label}' ending",
            "This ending is cheap or free and is used far more often than "
            "average in scams. Many sites using it are still harmless.", 8,
            "." + last_label)

    if registered in URL_SHORTENERS:
        add("url_shortener", "medium", "Shortened link hides the destination",
            "You cannot see where this link really goes until you open it.", 15,
            registered)

    if registered in FREE_HOSTING:
        add("free_hosting", "low", "Hosted on a free page-hosting service",
            "Anyone can publish a page on this service, so the name tells you "
            "little about who runs the page.", 8, registered)

    # --- brand impersonation ----------------------------------------------
    # Compare using the DECODED (human-visible) names, not the 'xn--' form.
    decoded_label = _decode_host(domain_label)[0]
    decoded_sub = _decode_host(subdomain)[0] if subdomain else ""
    official_brand = None
    brand_finding = None
    for brand in BRANDS:
        if registered and _is_official(brand, registered, decoded_label, suffix):
            official_brand = brand
            continue
        official_list = ", ".join(sorted(brand.official))
        in_sub = _brand_match(decoded_sub, brand) if decoded_sub else None
        in_dom = _brand_match(decoded_label, brand)
        skeleton_dom = _skeleton(decoded_label)
        hit = None
        if skeleton_dom == brand.label and decoded_label != brand.label:
            hit = (45, "Look-alike of " + brand.name,
                   f"The site name imitates '{brand.name}' using look-alike "
                   "characters. The real site is " + official_list + ".",
                   decoded_label)
        elif (len(brand.label) >= 5 and skeleton_dom != brand.label
              and _distance_leq_one(skeleton_dom, brand.label)):
            hit = (40, "Misspelled copy of " + brand.name,
                   f"The site name is one character away from '{brand.name}'. "
                   "Scammers register misspelled names hoping you will not "
                   "notice. The real site is " + official_list + ".",
                   decoded_label)
        elif in_sub == "exact":
            hit = (40, f"Uses the name '{brand.name}' in the sub-name",
                   f"The address starts with '{brand.name}' but the real site "
                   f"is '{registered or host}', not {official_list}. Only the "
                   "part just before the final ending shows who owns a site.",
                   decoded_sub[:80])
        elif in_dom == "exact":
            hit = (40, f"Uses the name '{brand.name}' but is not {brand.name}",
                   f"The site name contains '{brand.name}', yet this is not "
                   f"its official website ({official_list}).", decoded_label)
        elif in_sub == "prefix" or in_dom == "prefix":
            hit = (20 if in_dom == "prefix" else 30,
                   f"Name starts like '{brand.name}'",
                   f"Part of the address begins with '{brand.name}', but this "
                   f"is not its official website ({official_list}). Many "
                   "genuine fan or news sites do this too, so check carefully.",
                   (decoded_label if in_dom == "prefix" else decoded_sub)[:80])
        if hit and (brand_finding is None or hit[0] > brand_finding[0]):
            brand_finding = hit + (brand,)
    if brand_finding:
        weight, title, explanation, evidence, brand = brand_finding
        add("brand_impersonation", "high" if weight >= 40 else "medium",
            title, explanation, weight, evidence)
    elif official_brand:
        add("official_domain", "info", f"Matches the official {official_brand.name} website",
            "The site name matches a known official domain. This does not "
            "prove that this specific page or link is safe.", 0, registered)

    # --- words and path ----------------------------------------------------
    path = parts.path or ""
    if not official_brand:
        words = sorted(set(_tokens(host + " " + path)) & SENSITIVE_WORDS)
        if words:
            add("sensitive_words", "medium", "Asks about accounts, logins or payments",
                "Scam pages often use words like login, verify or confirm to "
                "make you hand over details.", min(10 + 3 * (len(words) - 1), 18),
                ", ".join(words[:6]))
        if brand_finding is None:
            for brand in BRANDS:
                if _brand_match(path, brand) == "exact" and (not registered or not _is_official(brand, registered, decoded_label, suffix)):
                    add("brand_in_path", "low", f"Mentions {brand.name} in the page path",
                        f"This is not {brand.name}'s own site, but its name "
                        "appears in the link.", 10, brand.name)
                    break

    if explicit_scheme == "http":
        add("no_https", "low", "Connection is not encrypted",
            "The link uses plain 'http'. Anything typed on the page can be "
            "read by others on the network.", 8)

    # --- query tricks -------------------------------------------------------
    try:
        for key, value in parse_qsl(parts.query, keep_blank_values=False):
            if key.lower() in REDIRECT_PARAMS and ("://" in value or value.lower().startswith("www.")):
                add("redirect_param", "medium", "Forwards you to another address",
                    "The link contains another web address and may send you "
                    "there after you click.", 12, f"{key}=...")
                break
    except ValueError:
        pass

    # --- files ----------------------------------------------------------------
    last_segment = path.rsplit("/", 1)[-1].lower()
    extension = last_segment.rsplit(".", 1)[-1] if "." in last_segment else ""
    if extension in RISKY_FILE_EXTENSIONS:
        extra = (
            " Installing apps from outside Google Play is a common way "
            "scammers spread fake loan and banking apps."
            if extension == "apk" else ""
        )
        add("risky_download", "high", f"Downloads a '.{extension}' file",
            "This link downloads a program or installer." + extra, 25, "." + extension)
    elif extension in ARCHIVE_EXTENSIONS:
        add("archive_download", "low", f"Downloads a '.{extension}' archive",
            "Archives can hide harmful files inside.", 10, "." + extension)

    # --- obfuscation -----------------------------------------------------------
    if "%" in host:
        add("encoded_host", "high", "Site name is hidden with % codes",
            "A real site name never needs % codes. They are used to disguise "
            "the address.", 15)
    elif re.search(r"%25[0-9a-fA-F]{2}", text):
        add("double_encoding", "low", "Repeated encoding in the link",
            "The link is encoded twice, a trick used to slip past filters.", 8)

    if len(text) > 200:
        add("long_url", "low", "Unusually long link",
            "Very long links can bury the part that matters.", 5, f"{len(text)} characters")

    return UrlAnalysis(text, normalized, host, registered, score_from(findings), findings)
