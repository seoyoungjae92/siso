from __future__ import annotations

import logging
import os
import time

import psycopg

from .embedding import SentenceTransformerEmbeddingProvider
from .fetch import CrawlNotAllowed
from .fetch import check_dead_link as _check_dead_link
from .fetch import check_robots_allowed as _check_robots_allowed
from .fetch import fetch_feed as _fetch_feed
from .fetch import fetch_robots_parser as _fetch_robots_parser
from .linkcheck import scan_dead_links
from .llm_client import (
    build_post_political_classifier,
    build_post_summarizer,
    build_topic_synthesizer,
)
from .matching import embed_pending_posts, match_pending_posts, prune_stale_candidates
from .matching_repository import PsycopgMatchingRepository
from .models import Source
from .pipeline import ingest_source
from .repository import PsycopgPostRepository
from .settings_repository import CrawlSettings, PsycopgSettingsRepository
from .sources_repository import PsycopgSourceRepository, SourceRepository
from .topic_synthesis import synthesize_pending_topics

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def run_cycle(
    sources: list[Source],
    settings: CrawlSettings,
    post_repo,
    matching_repo,
    embedder,
    source_repo: SourceRepository,
    check_robots_allowed=_check_robots_allowed,
    fetch_feed=_fetch_feed,
    check_dead_link=_check_dead_link,
    fetch_robots_parser=_fetch_robots_parser,
    topic_synthesizer=None,
    summarizer=None,
    political_classifier=None,
) -> None:
    for source in sources:
        if not source.feed_url:
            logger.info("소스 건너뜀(feed_url 없음): %s", source.name)
            continue

        try:
            min_interval = check_robots_allowed(source.feed_url)
            raw_bytes = fetch_feed(source.feed_url)
        except CrawlNotAllowed as exc:
            logger.warning("소스 건너뜀(robots.txt 불허): %s — %s", source.name, exc)
            _record_failure_and_maybe_disable(source_repo, source, settings.source_failure_threshold)
            continue
        except Exception as exc:  # noqa: BLE001 - 한 소스 실패로 전체 배치가 멈추면 안 됨
            logger.warning("소스 건너뜀(수집 실패): %s — %s", source.name, exc)
            _record_failure_and_maybe_disable(source_repo, source, settings.source_failure_threshold)
            continue

        source_repo.record_success(source.id)

        result = ingest_source(
            source, raw_bytes, post_repo, summarizer=summarizer, political_classifier=political_classifier
        )
        logger.info(
            "%s: fetched=%d inserted=%d skipped_duplicate=%d skipped_non_political=%d",
            source.name,
            result.fetched,
            result.inserted,
            result.skipped_duplicate,
            result.skipped_non_political,
        )

        time.sleep(min_interval)

    # 매칭/정리/데드링크/합성은 서로 독립적으로 DB를 다시 조회하는
    # 단계라 하나가 실패해도 나머지를 막을 이유가 없다 — 예전엔 여기
    # 하나만 죽어도 사이클 전체가 중단돼서, 그날 데드링크 정리처럼
    # 멀쩡히 돌 수 있었던 단계까지 못 도는 문제가 있었음.
    try:
        embedded = embed_pending_posts(matching_repo, embedder)
        logger.info("임베딩 계산: %d건", embedded)
    except Exception as exc:  # noqa: BLE001 - 한 단계 실패로 나머지 단계가 멈추면 안 됨
        logger.warning("임베딩 계산 실패: %s", exc)

    try:
        matched = match_pending_posts(matching_repo, threshold=settings.match_similarity_threshold)
        logger.info("매칭: %d쌍 생성", matched)
    except Exception as exc:  # noqa: BLE001
        logger.warning("매칭 실패: %s", exc)

    try:
        pruned = prune_stale_candidates(
            matching_repo,
            grace_period_hours=settings.grace_period_hours,
            min_cluster_size=settings.min_cluster_size,
            limit=settings.prune_scan_limit,
            prune_threshold=settings.prune_similarity_threshold,
        )
        logger.info("정리(prune): %d건 삭제", pruned)
    except Exception as exc:  # noqa: BLE001
        logger.warning("정리(prune) 실패: %s", exc)

    try:
        deleted = scan_dead_links(
            matching_repo,
            display_window_days=settings.display_window_days,
            limit=settings.dead_link_scan_limit,
            fetch_robots_parser=fetch_robots_parser,
            check_dead_link=check_dead_link,
        )
        logger.info("데드링크 정리: %d건 삭제", deleted)
    except Exception as exc:  # noqa: BLE001
        logger.warning("데드링크 정리 실패: %s", exc)

    if topic_synthesizer is not None:
        try:
            synthesized = synthesize_pending_topics(
                matching_repo, topic_synthesizer, limit=settings.synthesis_limit
            )
            logger.info("주제 합성: %d건", synthesized)
        except Exception as exc:  # noqa: BLE001
            logger.warning("주제 합성 실패: %s", exc)
    else:
        logger.info("주제 합성 건너뜀 (OPENROUTER_API_KEY 미설정)")


def _record_failure_and_maybe_disable(source_repo: SourceRepository, source: Source, threshold: int) -> None:
    """CLAUDE.md §4.2: 실패/차단(robots.txt 불허 포함) 감지 시 해당 소스
    자동 비활성화 + 관리자 알림. threshold회 연속 실패해야 비활성화하는
    이유는, 원 사이트의 일시적 장애 한 번으로 정상 소스가 꺼지는 걸
    막기 위함(다른 배치들의 "일시 장애는 삭제/차단으로 오판하지 않는다"
    원칙과 동일)."""
    count = source_repo.record_failure(source.id)
    if count >= threshold:
        logger.warning("소스 자동 비활성화(연속 %d회 실패): %s", count, source.name)
        source_repo.disable_and_alert(source.id, source.name, count)


def main() -> None:
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        source_repo = PsycopgSourceRepository(conn)
        sources = source_repo.find_enabled()
        post_repo = PsycopgPostRepository(conn)
        matching_repo = PsycopgMatchingRepository(conn)
        embedder = SentenceTransformerEmbeddingProvider()
        api_key = os.environ.get("OPENROUTER_API_KEY")
        topic_synthesizer = build_topic_synthesizer(api_key, model=settings.synthesis_model)
        summarizer = build_post_summarizer(api_key, model=settings.synthesis_model)
        political_classifier = build_post_political_classifier(api_key, model=settings.synthesis_model)
        run_cycle(
            sources,
            settings,
            post_repo,
            matching_repo,
            embedder,
            source_repo,
            topic_synthesizer=topic_synthesizer,
            summarizer=summarizer,
            political_classifier=political_classifier,
        )


if __name__ == "__main__":
    main()
