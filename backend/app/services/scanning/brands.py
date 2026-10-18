"""
Brands that scammers commonly impersonate, with their OFFICIAL domains.

This is DATA, meant to be reviewed and extended. Only list a domain here if
you are certain it belongs to the brand: a wrong entry makes the scanner
trust an impostor, a missing entry only makes it warn about a genuine site.

  label                 the word scammers put in fake addresses
  official              registered domains that really belong to the brand
  allow_country_domains True for global brands that run genuine regional sites
                        (google.co.ke, amazon.co.uk ...): `<label>.<ccTLD>`
                        is then treated as probably official.
"""
from dataclasses import dataclass


@dataclass(frozen=True)
class Brand:
    name: str
    label: str
    official: frozenset[str]
    allow_country_domains: bool = False


def _b(name, label, official, country=False):
    return Brand(name, label, frozenset(official), country)


BRANDS: tuple[Brand, ...] = (
    # --- Kenya: mobile money, banks, government --------------------------
    _b("Safaricom", "safaricom", ["safaricom.co.ke", "safaricom.com"]),
    _b("M-PESA", "mpesa", ["safaricom.co.ke", "m-pesa.com"]),
    _b("Airtel", "airtel", ["airtel.co.ke", "airtel.africa"]),
    _b("Equity Bank", "equitybank", ["equitybank.co.ke", "equitygroupholdings.com"]),
    _b("KCB", "kcb", ["kcbgroup.com", "kcbbankgroup.com"]),
    _b("Co-operative Bank", "coopbank", ["co-opbank.co.ke"]),
    _b("Absa", "absa", ["absa.co.ke", "absa.africa"]),
    _b("Stanbic", "stanbic", ["stanbicbank.co.ke"]),
    _b("NCBA", "ncba", ["ncbagroup.com"]),
    _b("KRA", "kra", ["kra.go.ke"]),
    _b("eCitizen", "ecitizen", ["ecitizen.go.ke"]),
    _b("NTSA", "ntsa", ["ntsa.go.ke"]),
    # --- Global ----------------------------------------------------------
    _b("PayPal", "paypal", ["paypal.com"], True),
    _b("Google", "google", ["google.com", "goo.gl", "gmail.com", "youtube.com"], True),
    _b("Microsoft", "microsoft", ["microsoft.com", "live.com", "office.com", "outlook.com"], True),
    _b("Apple", "apple", ["apple.com", "icloud.com"], True),
    _b("Amazon", "amazon", ["amazon.com", "amazonaws.com"], True),
    _b("Facebook", "facebook", ["facebook.com", "fb.com", "meta.com"], True),
    _b("Instagram", "instagram", ["instagram.com"], True),
    _b("WhatsApp", "whatsapp", ["whatsapp.com", "whatsapp.net"], True),
    _b("Netflix", "netflix", ["netflix.com"], True),
    _b("Binance", "binance", ["binance.com"], True),
    _b("DHL", "dhl", ["dhl.com"], True),
    _b("FedEx", "fedex", ["fedex.com"], True),
)

# Free hosting / tunnelling services: anyone can publish a page there.
FREE_HOSTING = frozenset({
    "github.io", "pages.dev", "workers.dev", "vercel.app", "netlify.app",
    "blogspot.com", "weebly.com", "wixsite.com", "web.app", "firebaseapp.com",
    "glitch.me", "ngrok.io", "ngrok-free.app", "trycloudflare.com",
    "duckdns.org", "000webhostapp.com", "herokuapp.com", "onrender.com",
    "repl.co", "replit.dev",
})

URL_SHORTENERS = frozenset({
    "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly", "buff.ly",
    "cutt.ly", "rebrand.ly", "shorturl.at", "tiny.cc", "rb.gy", "lnkd.in",
    "t.ly", "s.id", "v.gd", "bl.ink", "short.io",
})

# Endings that are disproportionately used for abuse (a weak signal only).
SUSPICIOUS_TLDS = frozenset({
    "zip", "mov", "top", "xyz", "click", "link", "gq", "tk", "ml", "cf", "ga",
    "work", "country", "kim", "loan", "men", "review", "stream", "download",
    "icu", "cyou", "buzz", "monster", "rest", "support",
})

SENSITIVE_WORDS = frozenset({
    "login", "signin", "verify", "verification", "secure", "security",
    "account", "update", "confirm", "password", "wallet", "otp", "pin",
    "reversal", "refund", "claim", "prize", "bonus", "unlock", "suspended",
    "banking", "payment", "billing", "invoice", "recover", "recovery",
})

REDIRECT_PARAMS = frozenset({
    "url", "redirect", "redirect_uri", "redirect_url", "redir", "next",
    "goto", "return", "returnurl", "return_url", "dest", "destination",
    "continue", "target", "link", "u",
})

DANGEROUS_SCHEMES = frozenset({
    "javascript", "data", "vbscript", "file", "blob", "about",
})

RISKY_FILE_EXTENSIONS = frozenset({
    "apk", "exe", "scr", "msi", "bat", "cmd", "vbs", "jar", "iso",
    "dmg", "pkg", "ps1", "lnk", "hta",
})
ARCHIVE_EXTENSIONS = frozenset({"zip", "rar", "7z"})
