"""
Guards against an incomplete dependency list.

The async database layer needs `greenlet`. SQLAlchemy 2.0.36 installs it automatically only up to
Python 3.12, so a clean Python 3.13 install once had a broken database layer that the local test run
did not notice (database tests are skipped on a development database). This test needs no database.
"""
from sqlalchemy.util import concurrency


def test_async_sqlalchemy_has_its_greenlet_dependency():
    assert concurrency.have_greenlet, (
        "greenlet is not installed, so async SQLAlchemy will fail on first use. "
        "Run: pip install -r requirements.txt"
    )
