import logging
from collections.abc import Callable
from dataclasses import dataclass

from .dedupe import hash_url
from .html_parsers import get_html_parser
from .llm_client import (
    PoliticalClassificationFailed,
    PostPoliticalClassifier,
    PostSummarizer,
)
from .models import Source
from .parser import RawEntry, parse_feed
from .profanity import contains_banned_word
from .repository import PostRepository
from .rss_fixups import get_rss_fixup
from .summarize import summarize

logger = logging.getLogger(__name__)


@dataclass
class IngestResult:
    fetched: int = 0
    inserted: int = 0
    skipped_duplicate: int = 0
    skipped_non_political: int = 0
    skipped_profanity: int = 0


def parse_entries(source: Source, raw_bytes: bytes) -> list[RawEntry]:
    if source.crawl_type == "rss":
        entries = parse_feed(raw_bytes)
        fixup = get_rss_fixup(source.feed_url)
        if fixup is not None:
            entries = [fixup(entry) for entry in entries]
        return entries

    if source.crawl_type == "html":
        parser = get_html_parser(source.feed_url)
        if parser is None:
            raise ValueError(f"등록된 HTML 파서가 없는 소스: {source.name} ({source.feed_url})")
        return parser(raw_bytes)

    raise ValueError(f"알 수 없는 crawl_type: {source.crawl_type}")


def ingest_source(
    source: Source,
    raw_bytes: bytes,
    repo: PostRepository,
    summarizer: PostSummarizer | None = None,
    political_classifier: PostPoliticalClassifier | None = None,
    fetch_detail_text: Callable[[str], str] | None = None,
) -> IngestResult:
    result = IngestResult()
    entries = parse_entries(source, raw_bytes)

    if not entries and source.crawl_type == "html":
        # 200 OK를 받았어도(fetch 자체는 성공 처리돼 consecutive_failures가
        # 안 늘어남) 파싱 결과가 0건이면 원인 파악용으로 응답 앞부분을
        # 남긴다 — 봇 차단 안내 페이지 등 목록과 다른 내용을 받았을 때
        # 진단할 방법이 전혀 없었음(2026-08-14, 디시인사이드 4개 소스
        # 전부 20시간 넘게 0건 발생 사례로 발견).
        logger.warning(
            "%s: 파싱 결과 0건(응답 %d바이트) — 앞부분: %r",
            source.name,
            len(raw_bytes),
            raw_bytes[:300].decode("utf-8", errors="replace"),
        )

    for entry in entries:
        result.fetched += 1
        if not entry.link:
            continue

        url_hash = hash_url(entry.link)
        if repo.exists_by_hash(url_hash):
            result.skipped_duplicate += 1
            continue

        # 목록 페이지 파서는 항상 summary=""를 준다(제목만 있음) — 상세
        # 페이지 파서가 등록된 사이트면 여기서 본문을 따로 가져와 그걸로
        # 요약을 만든다. 실패해도(사이트 미지원/네트워크 에러/레이트리밋
        # 상한) 빈 문자열이 오므로, 그럴 땐 entry.summary로 폴백한다(RSS
        # 소스는 feedparser가 이미 실제 요약을 주므로 그걸 덮어쓰면 안 됨).
        raw_body = (fetch_detail_text(entry.link) if fetch_detail_text is not None else "") or entry.summary
        summarized = summarize(raw_body, title=entry.title, summarizer=summarizer)

        # LLM 순화는 API 장애·레이트리밋이면 조용히 원문으로 폴백하므로,
        # 그 경우에도 항상 도는 로컬 사전 필터를 최후 방어선으로 둔다
        # (실측으로 확인 — LLM이 막혀있을 때 욕설이 그대로 저장된 사례 있었음).
        if contains_banned_word(summarized.title) or contains_banned_word(summarized.summary):
            result.skipped_profanity += 1
            continue

        if political_classifier is not None:
            try:
                if not political_classifier.is_political(summarized.title, summarized.summary):
                    result.skipped_non_political += 1
                    continue
            except PoliticalClassificationFailed as exc:
                logger.warning("정치성 판단 실패, 안전하게 저장: %s", exc)

        repo.insert_post(
            source_id=source.id,
            title=summarized.title,
            summary=summarized.summary,
            origin_url=entry.link,
            origin_url_hash=url_hash,
            published_at=entry.published_at,
        )
        result.inserted += 1

    return result
