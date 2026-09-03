from pathlib import Path

import pytest

from siso_crawler.dedupe import hash_url
from siso_crawler.models import Source
from siso_crawler.pipeline import ingest_source
from siso_crawler.summarize import SUMMARY_MAX_LEN

from .fakes import FakePostPoliticalClassifier, FakePostRepository, FakePostSummarizer

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


def test_ingest_source_uses_fetch_detail_text_for_summary_when_provided(sample_feed_bytes):
    # 목록 파서는 항상 summary=""를 주므로, 상세 파서가 있는 사이트면
    # fetch_detail_text로 받아온 본문이 요약에 실제로 쓰여야 한다.
    calls: list[str] = []

    def fetch_detail_text(url: str) -> str:
        calls.append(url)
        return f"본문: {url}"

    repo = FakePostRepository()

    result = ingest_source(SOURCE, sample_feed_bytes, repo, fetch_detail_text=fetch_detail_text)

    assert result.inserted == 2
    assert calls == [
        "https://example-community.test/posts/1",
        "https://example-community.test/posts/2",
    ]
    assert repo.inserted[0]["summary"] == "본문: https://example-community.test/posts/1"


def test_ingest_source_falls_back_to_entry_summary_when_fetch_detail_text_yields_nothing(
    sample_feed_bytes,
):
    # RSS 소스는 feedparser가 이미 실제 요약을 주는데, fetch_detail_text가
    # 이 사이트엔 상세 파서가 없어 빈 문자열을 돌려주는 경우(2026-09
    # 발견 — 무조건 덮어쓰면 RSS 소스의 기존 요약이 사라지는 회귀였음).
    repo = FakePostRepository()

    result = ingest_source(SOURCE, sample_feed_bytes, repo, fetch_detail_text=lambda url: "")

    assert result.inserted == 2
    assert repo.inserted[0]["summary"] != ""


def test_ingest_source_without_fetch_detail_text_keeps_entry_summary(sample_feed_bytes):
    # fetch_detail_text 미지정(상세 파서 없는 사이트)이면 지금까지처럼
    # entry.summary(RSS면 피드에 있는 description, HTML 파서면 항상 빈
    # 문자열)를 그대로 쓴다 — fetch_detail_text 쪽 값이 섞이면 안 됨.
    repo = FakePostRepository()

    result = ingest_source(SOURCE, sample_feed_bytes, repo)

    assert result.inserted == 2
    assert not repo.inserted[0]["summary"].startswith("본문:")


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


def test_ingest_source_logs_response_snippet_when_html_parse_yields_nothing(caplog):
    html_source = Source(
        id=2,
        name="오늘의유머",
        side="left",
        base_url="https://www.todayhumor.co.kr",
        feed_url="https://www.todayhumor.co.kr/board/list.php?table=bestofbest",
        crawl_type="html",
        enabled=True,
    )
    repo = FakePostRepository()

    with caplog.at_level("WARNING"):
        result = ingest_source(
            html_source, "<html><body>차단된 접근입니다</body></html>".encode(), repo
        )

    assert result.fetched == 0
    assert any("파싱 결과 0건" in record.message and "차단된 접근입니다" in record.message for record in caplog.records)


def test_ingest_source_does_not_log_when_entries_found(sample_feed_bytes, caplog):
    repo = FakePostRepository()

    with caplog.at_level("WARNING"):
        ingest_source(SOURCE, sample_feed_bytes, repo)

    assert not any("파싱 결과 0건" in record.message for record in caplog.records)


def test_ingest_source_passes_title_and_summarizer_through_to_summarize(sample_feed_bytes):
    repo = FakePostRepository()
    summarizer = FakePostSummarizer()

    result = ingest_source(SOURCE, sample_feed_bytes, repo, summarizer=summarizer)

    assert result.inserted == 2
    assert len(summarizer.calls) == 2
    assert all(p["summary"].startswith("재작성: ") for p in repo.inserted)
    assert all(p["title"].startswith("재작성: ") for p in repo.inserted)


def test_ingest_source_skips_posts_containing_banned_word():
    # 실측으로 확인된 케이스: LLM 순화가 실패(레이트리밋 등)하면 원문
    # 그대로 폴백하는데, 이때 로컬 사전 필터가 최후 방어선으로 걸러야 한다.
    feed = """<?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0"><channel>
      <item>
        <title>창녀</title>
        <link>https://example-community.test/posts/1</link>
        <description>본문 없음</description>
      </item>
    </channel></rss>""".encode()
    repo = FakePostRepository()

    result = ingest_source(SOURCE, feed, repo)

    assert result.fetched == 1
    assert result.inserted == 0
    assert result.skipped_profanity == 1
    assert repo.inserted == []


def test_ingest_source_skips_posts_classified_as_non_political(sample_feed_bytes):
    repo = FakePostRepository()
    classifier = FakePostPoliticalClassifier(non_political={"첫 번째 테스트 게시글 제목입니다"})

    result = ingest_source(SOURCE, sample_feed_bytes, repo, political_classifier=classifier)

    assert result.fetched == 2
    assert result.inserted == 1
    assert result.skipped_non_political == 1
    assert len(repo.inserted) == 1
    assert repo.inserted[0]["title"] == "두 번째 테스트 게시글"


def test_ingest_source_keeps_post_when_classifier_fails(sample_feed_bytes):
    repo = FakePostRepository()
    classifier = FakePostPoliticalClassifier(fail_on={"첫 번째 테스트 게시글 제목입니다"})

    result = ingest_source(SOURCE, sample_feed_bytes, repo, political_classifier=classifier)

    assert result.inserted == 2
    assert result.skipped_non_political == 0
    assert len(classifier.calls) == 2


def test_ingest_source_applies_rss_fixup_for_registered_feed_url(sample_feed_bytes):
    clien_source = Source(
        id=4,
        name="클리앙 인기글",
        side="left",
        base_url="https://www.clien.net",
        feed_url="https://feeds.feedburner.com/clien_hot10_rss",
        crawl_type="rss",
        enabled=True,
    )
    repo = FakePostRepository()

    result = ingest_source(clien_source, sample_feed_bytes, repo)

    assert result.inserted == 2
    # fixture의 "Tue, 21 Jul 2026 09:00:00 GMT"는 실제로는 KST라 UTC로는
    # 9시간 이전(00:00:00)이어야 한다 — GMT 라벨을 그대로 믿으면 안 됨.
    assert repo.inserted[0]["published_at"] == "2026-07-21T00:00:00+00:00"


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
