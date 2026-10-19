"""The signing secret must be strong where it matters, and never leak into messages."""
import logging

import pytest
from pydantic import ValidationError

from app.core.config import Settings

GOOD = "k3Jx9-Qm2vLw8TzR5nYbH7cUeA1pDfGs4oIiXjV0tWq6ZaBr"   # 48 characters
SHORT = "shortsecret123"


def _settings(**kwargs):
    return Settings(_env_file=None, RESEND_API_KEY="x", **kwargs)


@pytest.mark.parametrize("weak", [SHORT, "x" * 31, "changeme" + "a" * 40, "Secret" + "b" * 40, "password" + "c" * 40])
def test_weak_secrets_are_refused_in_production(weak):
    with pytest.raises(ValidationError) as excinfo:
        _settings(JWT_SECRET_KEY=weak, ENVIRONMENT="production")
    assert "JWT_SECRET_KEY is weak" in str(excinfo.value)
    assert weak not in str(excinfo.value)                    # the secret itself is never echoed back


def test_strong_secret_is_accepted_in_production():
    assert _settings(JWT_SECRET_KEY=GOOD, ENVIRONMENT="production").JWT_SECRET_KEY == GOOD


def test_weak_secret_only_warns_in_development_and_never_logs_the_value(caplog):
    with caplog.at_level(logging.WARNING, logger="aggreyshield.config"):
        s = _settings(JWT_SECRET_KEY=SHORT, ENVIRONMENT="development")
    assert s.JWT_SECRET_KEY == SHORT                          # development keeps working
    assert any("JWT_SECRET_KEY is weak" in r.message for r in caplog.records)
    assert SHORT not in caplog.text


def test_strong_secret_logs_no_warning(caplog):
    with caplog.at_level(logging.WARNING, logger="aggreyshield.config"):
        _settings(JWT_SECRET_KEY=GOOD, ENVIRONMENT="development")
    assert "weak" not in caplog.text
