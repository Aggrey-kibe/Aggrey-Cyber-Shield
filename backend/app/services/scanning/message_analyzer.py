"""
Explainable analysis of an SMS / email / chat message.

Two layers: (1) scam-language patterns in the text (English and some Swahili,
including common Kenyan M-PESA scams), and (2) every link in the message is run
through the URL analyzer. Offline and pure: nothing is opened or fetched.

All patterns use BOUNDED quantifiers and no nested repetition so that hostile
5,000-character messages cannot cause catastrophic regex backtracking.
"""
import re
from dataclasses import dataclass, field

from app.services.scanning.types import Finding, UrlAnalysis, score_from
from app.services.scanning.url_analyzer import InvalidUrl, analyze_url

MAX_MESSAGE_LENGTH = 5000
MAX_URLS = 10

_URL_RE = re.compile(
    r"(?i)(?:https?://|www\.)[^\s<>\"']{1,500}"
    r"|\b(?:[a-z0-9-]{1,63}\.){1,5}"
    r"(?:co\.ke|or\.ke|go\.ke|ac\.ke|com|net|org|ke|io|me|xyz|top|click|link|"
    r"app|info|biz|tk|ml|ga|cf|gq|ly|gl|ws|cc|tv|africa|online|site|shop|"
    r"club|vip|zip|mov)\b(?:/[^\s<>\"']{0,500})?"
)
_TRAILING_PUNCT = ".,;:!?)]}>'\""
_NEGATION_RE = re.compile(
    r"(do not|don't|dont|never|won't|will not|usishiriki|usitume)\s*\w{0,12}\s*$"
)


@dataclass(frozen=True)
class _Rule:
    id: str
    severity: str
    title: str
    explanation: str
    weight: int
    patterns: tuple[str, ...]
    negatable: bool = False  # ignore matches that are warnings ("never share...")


_RULES: tuple[_Rule, ...] = (
    _Rule(
        "asks_secret", "high", "Asks for a PIN, password or one-time code",
        "No genuine bank, M-PESA or company will ask you to send or read out "
        "your PIN, password or one-time code, not by SMS, call or chat.", 50,
        (
            r"\b(?:send|share|give|provide|enter|confirm|reply with|tell|read out)\b[^.\n]{0,40}\b(?:pin|otp|password|passcode|cvv|one[- ]time (?:code|password)|verification code|secret code)\b",
            r"\bnamba ya siri\b|\bnambari ya siri\b",
        ),
        negatable=True,
    ),
    _Rule(
        "money_reversal", "high", "'Sent by mistake - please send it back' pattern",
        "A very common scam: someone claims to have sent you money by mistake "
        "and asks you to return it. The original payment is often fake, or "
        "later reversed, so you lose your own money.", 30,
        (
            r"\b(?:sent|send|transferred|deposited)\b[^.\n]{0,40}\b(?:by mistake|in error|wrongly|wrong number|wrong person)\b",
            r"\b(?:reverse|return|send back|refund)\b[^.\n]{0,40}\b(?:the money|the amount|ksh|kes|funds|transaction)\b",
            r"\bnimetuma\b[^.\n]{0,30}\bkwa makosa\b",
            r"\btafadhali\b[^.\n]{0,20}\b(?:rudisha|nirudishie|nitumie)\b",
        ),
    ),
    _Rule(
        "prize_claim", "high", "Says you have won a prize",
        "Unexpected prizes, lotteries and 'lucky draws' are scams. You cannot "
        "win a contest you never entered.", 25,
        (
            r"\byou(?:'ve| have)? won\b",
            r"\bcongratulations\b[^.\n]{0,40}\b(?:won|winner|selected)\b",
            r"\bclaim your (?:prize|reward|gift|winnings)\b",
            r"\blucky (?:winner|draw)\b",
            r"\bumeshinda\b|\bzawadi yako\b",
        ),
    ),
    _Rule(
        "advance_fee", "high", "Asks you to pay a fee first",
        "Real loans, jobs and prizes do not make you pay a 'registration', "
        "'processing' or 'release' fee before you receive them. This is the "
        "usual trick behind fake loan apps and job scams.", 25,
        (
            r"\b(?:registration|processing|clearance|release|admin|insurance|activation)\s+(?:fee|charge|deposit)\b",
            r"\bpay\b[^.\n]{0,40}\bto (?:get|receive|claim|release|secure)\b[^.\n]{0,20}\b(?:loan|prize|job|parcel|package|reward|interview)\b",
            r"\bdeposit\b[^.\n]{0,30}\b(?:first|before)\b[^.\n]{0,30}\b(?:loan|withdraw)\b",
        ),
    ),
    _Rule(
        "investment_scam", "high", "Promises guaranteed or huge returns",
        "Genuine investments cannot guarantee profit. 'Double your money' and "
        "fixed daily returns are signs of a pyramid or Ponzi scam.", 25,
        (
            r"\bguaranteed (?:returns?|profits?|income)\b",
            r"\bdouble your (?:money|investment)\b",
            r"\b\d{2,3}\s?% (?:daily|weekly|returns?|profit)\b",
        ),
    ),
    _Rule(
        "threat_account", "medium", "Threatens to block your account or take action",
        "Scammers frighten you into acting before you think. Real providers "
        "do not threaten account closure or arrest by SMS.", 15,
        (
            r"\b(?:account|sim|line|card|wallet)\b[^.\n]{0,40}\b(?:suspended|blocked|closed|deactivated|locked|frozen|restricted|terminated)\b",
            r"\b(?:legal action|arrested|arrest|prosecuted|prosecution)\b",
            r"\bakaunti yako\b[^.\n]{0,30}\b(?:itafungwa|imefungwa|itazuiwa)\b",
        ),
    ),
    _Rule(
        "verify_prompt", "medium", "Tells you to 'verify' or 'update' your details",
        "Scam messages often say you must verify, confirm or update your "
        "account or details so you will type them into a fake page.", 15,
        (
            r"\b(?:verify|confirm|update|validate|reactivate|unlock)\b[^.\n]{0,30}\b(?:your )?(?:account|details|information|identity|kyc|bank|card|number)\b",
            r"\b(?:verify|confirm|update|validate|reactivate|unlock)\b[^.\n]{0,12}\b(?:now|immediately|today|here|below|again)\b",
            r"\bthibitisha\b",
        ),
    ),
    _Rule(
        "install_app", "medium", "Asks you to install an app",
        "Installing an app from a link or outside Google Play is a common way "
        "scammers spread fake loan, banking and 'security' apps that steal "
        "data.", 15,
        (
            r"\b(?:download|install)\b[^.\n]{0,40}\b(?:app|apk|application)\b",
            r"\.apk\b",
        ),
    ),
    _Rule(
        "urgency", "medium", "Pushes you to act immediately",
        "Pressure to act 'now' is meant to stop you from checking.", 10,
        (
            r"\b(?:urgent(?:ly)?|immediately|right now|act now|last (?:warning|chance|notice)|final notice)\b",
            r"\bwithin (?:24|48|12|6) hours\b",
            r"\bexpires? (?:today|soon|tonight)\b",
            r"\bharaka\b|\bmara moja\b",
        ),
    ),
    _Rule(
        "payment_request", "low", "Asks for payment to a number or till",
        "Before paying anyone you do not know, check who owns the paybill, "
        "till or phone number.", 8,
        (
            r"\bpay ?bill\b",
            r"\btill (?:number|no)\b",
            r"\bsend\b[^.\n]{0,20}\b(?:money|cash)\b[^.\n]{0,20}\bto\b[^.\n]{0,10}(?:\+?254|0)[17]\d{8}\b",
        ),
    ),
)


def extract_urls(text: str) -> list[str]:
    seen: list[str] = []
    for match in _URL_RE.finditer(text):
        url = match.group(0).rstrip(_TRAILING_PUNCT)
        if url and url not in seen:
            seen.append(url)
        if len(seen) >= MAX_URLS:
            break
    return seen


def _snippet(text: str, start: int, end: int) -> str:
    return text[max(0, start - 15): min(len(text), end + 15)].strip()[:120]


@dataclass
class MessageAnalysis:
    score: int
    findings: list[Finding] = field(default_factory=list)
    urls: list[UrlAnalysis] = field(default_factory=list)


def analyze_message(raw: str) -> MessageAnalysis:
    if not isinstance(raw, str):
        raise InvalidUrl("A message must be text.")
    text = raw.strip()
    if not text:
        raise InvalidUrl("Please paste the message you want to check.")
    if len(text) > MAX_MESSAGE_LENGTH:
        raise InvalidUrl(f"That message is too long (maximum {MAX_MESSAGE_LENGTH} characters).")
    text = "".join(c for c in text if c in "\n\t" or (ord(c) >= 32 and ord(c) != 127))
    lowered = text.lower()

    findings: list[Finding] = []
    for rule in _RULES:
        for pattern in rule.patterns:
            hit = None
            for match in re.finditer(pattern, lowered):
                before = lowered[max(0, match.start() - 25): match.start()]
                if rule.negatable and _NEGATION_RE.search(before):
                    continue  # e.g. "never share your PIN"
                hit = match
                break
            if hit:
                findings.append(
                    Finding(rule.id, rule.severity, rule.title, rule.explanation,
                            rule.weight, _snippet(text, hit.start(), hit.end()))
                )
                break

    url_results: list[UrlAnalysis] = []
    for url in extract_urls(text):
        try:
            url_results.append(analyze_url(url))
        except InvalidUrl:
            continue  # text that merely looked like a link

    pressure_ids = {"threat_account", "verify_prompt", "urgency", "asks_secret",
                    "prize_claim", "advance_fee"}
    if url_results and sum(f.id in pressure_ids for f in findings) >= 2:
        findings.append(
            Finding("pressure_plus_link", "high",
                    "Pressure plus a link: the classic phishing recipe",
                    "Scare or urgency wording together with a link is how most "
                    "phishing messages work. Do not tap the link.", 10)
        )

    worst = max(url_results, key=lambda u: u.score, default=None)
    for u in url_results:
        if u.score >= 25:
            findings.append(
                Finding("risky_link", "high", "Contains a link that looks dangerous",
                        "A link in this message has warning signs (details below).",
                        0, u.normalized_url[:120])
            )
            break

    score = min(100, score_from(findings) + (worst.score if worst else 0))
    return MessageAnalysis(score=score, findings=findings, urls=url_results)
