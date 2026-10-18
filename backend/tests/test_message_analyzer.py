"""Message analyzer: genuine messages stay clean, scams are explained."""
import time

import pytest

from app.services.scanning.message_analyzer import (
    MAX_MESSAGE_LENGTH,
    analyze_message,
    extract_urls,
)
from app.services.scanning.types import verdict_for
from app.services.scanning.url_analyzer import InvalidUrl

BENIGN = [
    "Hi, are we still meeting at 5pm tomorrow?",
    "Your OTP is 482913. Do not share it with anyone.",
    "Safaricom: Your M-PESA balance is KES 1,250. Never share your PIN with anyone.",
    "Confirmed. KES 500 sent to JOHN. Dial *334# for help.",
    "Happy birthday! See you at the party on Saturday.",
    "Code review notes are at https://github.com/Aggrey-kibe/Aggrey-Cyber-Shield",
]


@pytest.mark.parametrize("text", BENIGN)
def test_genuine_messages_are_clean(text):
    result = analyze_message(text)
    assert result.score < 25, [f.id for f in result.findings]


SCAMS = [
    ("Hello I sent you KES 3000 by mistake. Please send it back to 0712345678 urgently.", "money_reversal"),
    ("Nimetuma pesa kwa makosa, tafadhali nirudishie haraka", "money_reversal"),
    ("Congratulations! You have won KES 100,000. Claim your prize now", "prize_claim"),
    ("Pay registration fee to get your loan released today", "advance_fee"),
    ("Invest 5000 and get guaranteed returns. Double your money!", "investment_scam"),
    ("Reply with your M-PESA PIN to unlock your account", "asks_secret"),
    ("Please send your OTP to confirm your identity", "asks_secret"),
    ("Your account will be suspended within 24 hours", "threat_account"),
    ("Please verify your account details to continue", "verify_prompt"),
    ("Download our app to receive your loan: loanfast.co.ke/app.apk", "install_app"),
]


@pytest.mark.parametrize("text,expected_id", SCAMS)
def test_scam_language_is_flagged(text, expected_id):
    ids = [f.id for f in analyze_message(text).findings]
    assert expected_id in ids, ids


def test_pin_request_alone_is_likely_dangerous():
    r = analyze_message("Reply with your M-PESA PIN to unlock your account")
    assert verdict_for(r.score)[0] == "likely_dangerous"


def test_pressure_plus_link_combination():
    r = analyze_message("Your KRA account will be suspended. Verify your details now: bit.ly/3xyz")
    ids = [f.id for f in r.findings]
    assert "pressure_plus_link" in ids
    assert verdict_for(r.score)[0] == "likely_dangerous"
    assert r.urls and r.urls[0].registered_domain == "bit.ly"


def test_dangerous_link_inside_message_raises_score_and_is_listed():
    r = analyze_message("see http://safaricom.co.ke.verify-account.xyz/login")
    assert any(f.id == "risky_link" for f in r.findings)
    assert r.score >= 50


def test_warnings_about_secrets_are_not_mistaken_for_requests():
    r = analyze_message("Never share your PIN or password with anyone, not even staff.")
    assert not any(f.id == "asks_secret" for f in r.findings)


def test_url_extraction():
    urls = extract_urls("visit www.example.com/a, or bit.ly/x1! also https://a.co.ke/p).")
    assert "www.example.com/a" in urls
    assert "bit.ly/x1" in urls
    assert "https://a.co.ke/p" in urls
    assert len(extract_urls(" ".join(f"http://x{i}.com" for i in range(50)))) == 10


@pytest.mark.parametrize("bad", ["", "   ", "x" * (MAX_MESSAGE_LENGTH + 1)])
def test_invalid_messages_rejected(bad):
    with pytest.raises(InvalidUrl):
        analyze_message(bad)


@pytest.mark.parametrize(
    "hostile",
    [
        "a" * 5000,
        "send " * 1000,
        "verify your " * 400,
        "http://" + "a." * 1200,
        "pay " + "x " * 1500 + "to get loan",
        "\x00" * 100 + "hello",
    ],
)
def test_hostile_messages_are_fast_and_safe(hostile):
    start = time.time()
    try:
        r = analyze_message(hostile)
        assert 0 <= r.score <= 100
    except InvalidUrl:
        pass
    assert time.time() - start < 1.0
