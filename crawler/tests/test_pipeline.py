from pathlib import Path

import pytest

from siso_crawler.dedupe import hash_url
from siso_crawler.models import Source
from siso_crawler.pipeline import ingest_source
from siso_crawler.summarize import SUMMARY_MAX_LEN

from .fakes import FakePostRepository

FIXTURES_DIR = Path(__file__).parent / "fixtures"

SOURCE = Source(
    id=1,
    name="Sample Community",
    side="left",
    base_url="https://example-community.test",
    feed_url="https://example-community.test/rss",
    crawl_type="rss",
    enabled=True,
)


def test_ingest_source_inserts_all_new_entries(sample_feed_bytes):
    repo = FakePostRepository()

    result = ingest_source(SOURCE, sample_feed_bytes, repo)

    assert result.fetched == 2
    assert result.inserted == 2
    assert result.skipped_duplicate == 0
    assert len(repo.inserted) == 2
    assert all(len(p["summary"]) <= SUMMARY_MAX_LEN for p in repo.inserted)
    assert all(p["source_id"] == SOURCE.id for p in repo.inserted)


def test_ingest_source_skips_existing_duplicates(sample_feed_bytes):
    existing_hash = hash_url("https://example-community.test/posts/1")
    repo = FakePostRepository(existing_hashes={existing_hash})

    result = ingest_source(SOURCE, sample_feed_bytes, repo)

    assert result.fetched == 2
    assert result.inserted == 1
    assert result.skipped_duplicate == 1
    assert repo.inserted[0]["origin_url"] == "https://example-community.test/posts/2"


def test_ingest_source_dispatches_to_html_parser_by_crawl_type():
    html_source = Source(
        id=2,
        name="오늘의유머",
        side="left",
        base_url="https://www.todayhumor.co.kr",
        feed_url="https://www.todayhumor.co.kr/board/list.php?table=bestofbest",
        crawl_type="html",
        enabled=True,
    )
    html_bytes = (FIXTURES_DIR / "todayhumor_list.html").read_bytes()
    repo = FakePostRepository()

    result = ingest_source(html_source, html_bytes, repo)

    assert result.fetched == 4
    assert result.inserted == 4
    assert all(p["source_id"] == html_source.id for p in repo.inserted)


def test_ingest_source_raises_on_unknown_crawl_type():
    bad_source = Source(
        id=3,
        name="알수없음",
        side="left",
        base_url="https://unknown.test",
        feed_url="https://unknown.test/list",
        crawl_type="json",
        enabled=True,
    )
    repo = FakePostRepository()

    with pytest.raises(ValueError):
        ingest_source(bad_source, b"", repo)
