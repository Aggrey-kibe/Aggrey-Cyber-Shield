"""URL analyzer: benign links stay clean, scam patterns are caught and explained."""
import random
import string
import time

import pytest

from app.services.scanning.types import verdict_for
from app.services.scanning.url_analyzer import InvalidUrl, analyze_url

BENIGN = [
    "https://www.google.com/search?q=cats",
    "https://accounts.google.com/signin",
    "https://mail.google.com/mail/u/0/",
    "https://www.amazon.co.ke/deals",
    "https://github.com/Aggrey-kibe/Aggrey-Cyber-Shield",
    "https://en.wikipedia.org/wiki/Nairobi",
    "https://www.safaricom.co.ke/personal/m-pesa",
    "https://itax.kra.go.ke/KRA-Portal/",
    "https://www.equitybank.co.ke/",
    "https://aggreykibe.blogspot.com/",
    "example.org/about",
]


@pytest.mark.parametrize("url", BENIGN)
def test_benign_links_have_no_serious_warning(url):
    result = analyze_url(url)
    assert result.score < 25, [f.id for f in result.findings]


DANGEROUS = [
    ("http://safaricom.co.ke.verify-account.xyz/login", "brand_impersonation"),
    ("http://paypa1-secure-login.com/verify", "brand_impersonation"),
    ("http://paypal.xyz", "brand_impersonation"),
    ("http://m-pesa-rewards.top/claim", "brand_impersonation"),
    ("http://equity-bank-loans.xyz/apply", "brand_impersonation"),
    ("https://kcb-bank-update.top/login", "brand_impersonation"),
    ("http://safarlcom.com", "brand_impersonation"),
    ("https://google.com@evil.example.com/", "userinfo_trick"),
    ("http://192.168.4.7/login", "ip_address"),
    ("http://3232235777/", "obfuscated_ip"),
    ("http://0xc0a80101/", "obfuscated_ip"),
    ("http://example.com/loan-app.apk", "risky_download"),
    ("javascript:alert(1)", "dangerous_scheme"),
    ("data:text/html,<script>alert(1)</script>", "dangerous_scheme"),
    ("https://bit.ly/3abc", "url_shortener"),
    ("http://example.com/go?url=http://evil.example.net", "redirect_param"),
]


@pytest.mark.parametrize("url,expected_id", DANGEROUS)
def test_scam_patterns_are_flagged_with_the_right_reason(url, expected_id):
    result = analyze_url(url)
    ids = [f.id for f in result.findings]
    assert expected_id in ids, ids
    assert result.score >= 15


def test_serious_impersonations_reach_likely_dangerous():
    for url in (
        "http://safaricom.co.ke.verify-account.xyz/login",
        "http://paypa1-secure-login.com/verify",
        "http://m-pesa-rewards.top/claim",
        "javascript:alert(1)",
    ):
        assert verdict_for(analyze_url(url).score)[0] == "likely_dangerous", url


def test_cyrillic_lookalike_domain_detected():
    fake = "https://" + "раураl".encode("idna").decode() + ".com"  # looks like paypal
    result = analyze_url(fake)
    ids = [f.id for f in result.findings]
    assert "brand_impersonation" in ids
    assert "mixed_scripts" in ids or "idn_host" in ids
    assert result.score >= 50


def test_prefix_match_on_unrelated_site_is_only_a_mild_warning():
    # a genuine tech-news site: must not be treated like apple-support scams
    assert analyze_url("https://appleinsider.com/articles").score < 25


def test_official_brand_domain_is_noted_but_never_called_safe():
    result = analyze_url("https://www.google.com/")
    assert any(f.id == "official_domain" for f in result.findings)
    assert result.to_dict()["verdict"] == "no_warning_signs"
    _, _, summary, _ = verdict_for(0)
    assert "NOT a guarantee" in summary


def test_every_finding_explains_itself():
    for url, _ in DANGEROUS:
        for f in analyze_url(url).findings:
            assert f.title and len(f.explanation) > 20


@pytest.mark.parametrize(
    "bad", ["", "   ", "http://", "http://[::1", "a" * 2049, "http://exa\x00mple.com", "https:///"]
)
def test_unanalysable_input_raises_invalid_url(bad):
    with pytest.raises(InvalidUrl):
        analyze_url(bad)


def test_non_string_input_rejected():
    with pytest.raises(InvalidUrl):
        analyze_url(12345)  # type: ignore[arg-type]


def test_never_opens_the_network(monkeypatch):
    import socket

    def boom(*a, **k):
        raise AssertionError("analysis must not touch the network")

    monkeypatch.setattr(socket, "socket", boom)
    monkeypatch.setattr(socket, "getaddrinfo", boom)
    analyze_url("http://paypa1-secure-login.com/verify")
    analyze_url("https://bit.ly/3abc")


def test_fuzz_random_input_never_crashes_and_stays_fast():
    rng = random.Random(1234)
    alphabet = string.printable + "аеоρ你好é%@:/\\.-_[]"
    start = time.time()
    for _ in range(1500):
        text = "".join(rng.choice(alphabet) for _ in range(rng.randint(1, 300)))
        try:
            result = analyze_url(text)
            assert 0 <= result.score <= 100
        except InvalidUrl:
            pass
    assert time.time() - start < 10


@pytest.mark.parametrize(
    "hostile",
    [
        "http://" + "a." * 900 + "com",
        "http://" + "-" * 1500 + ".com",
        "http://example.com/" + "%25" * 600,
        "http://xn--" + "a" * 200 + ".com",
        "http://user:pass@" * 60 + "evil.com",
        "http://[" + "1:" * 30 + "1]/",
    ],
)
def test_hostile_urls_handled_quickly(hostile):
    start = time.time()
    try:
        analyze_url(hostile)
    except InvalidUrl:
        pass
    assert time.time() - start < 1.0
