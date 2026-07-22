from pathlib import Path

import pytest

FIXTURES_DIR = Path(__file__).parent / "fixtures"


@pytest.fixture
def sample_feed_bytes() -> bytes:
    return (FIXTURES_DIR / "sample_feed.xml").read_bytes()
