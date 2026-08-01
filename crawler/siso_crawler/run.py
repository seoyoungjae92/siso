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


def run_ingest_cycle(
    sources: list[Source],
    settings: CrawlSettings,
    post_repo,
    source_repo: SourceRepository,
    check_robots_allowed=_check_robots_allowed,
    fetch_feed=_fetch_feed,
    summarizer=None,
    political_classifier=None,
) -> None:
    """글 하나당 LLM 호출(정치성 분류+요약)을 순차로 하다 보니 소스 하나에
    10분 이상 걸릴 수 있어, 좌/우 소스가 한 프로세스에 다 몰려있으면
    등록 순서가 앞선 쪽이 시간/메모리를 다 써버리고 뒤쪽은 사이클 내내
    한 번도 못 도는 문제가 실제로 발생함(2026-08-01, 우측 소스 전부
    미도달). `sources`를 호출부에서 미리 side로 걸러서 넘기면(main()의
    CRAWL_SIDE) 좌/우를 별도 프로세스/스케줄로 분리해 돌릴 수 있다."""
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


def run_postprocess_cycle(
    settings: CrawlSettings,
    matching_repo,
    embedder,
    check_dead_link=_check_dead_link,
    fetch_robots_parser=_fetch_robots_parser,
    topic_synthesizer=None,
) -> None:
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
        matched = match_pending_posts(
            matching_repo,
            threshold=settings.match_similarity_threshold,
            cohort_threshold=settings.cohort_similarity_threshold,
            min_posts_per_side=settings.synthesis_min_posts_per_side,
        )
        logger.info("매칭: %d쌍 생성", matched)
    except Exception as exc:  # noqa: BLE001
        logger.warning("매칭 실패: %s", exc)

    try:
        pruned = prune_stale_candidates(
            matching_repo,
            grace_period_hours=settings.grace_period_hours,
            min_cluster_size=settings.min_cluster_size,
            limit=settings.prune_scan_limit,
            match_similarity_threshold=settings.match_similarity_threshold,
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
    """수집(ingest)+후처리(postprocess)를 한 프로세스에서 전부 도는
    기존 동작 — 로컬 테스트/개발용으로 유지. 운영에서는 main()이
    CRAWL_MODE/CRAWL_SIDE로 둘을 분리해서 따로 돌린다(위 두 함수의
    docstring 참고)."""
    run_ingest_cycle(
        sources,
        settings,
        post_repo,
        source_repo,
        check_robots_allowed=check_robots_allowed,
        fetch_feed=fetch_feed,
        summarizer=summarizer,
        political_classifier=political_classifier,
    )
    run_postprocess_cycle(
        settings,
        matching_repo,
        embedder,
        check_dead_link=check_dead_link,
        fetch_robots_parser=fetch_robots_parser,
        topic_synthesizer=topic_synthesizer,
    )


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
    """CRAWL_MODE(미설정/ingest/postprocess)로 어느 단계를 돌지 고른다 —
    미설정이면 기존처럼 수집+후처리를 전부 한 프로세스에서 돈다(로컬
    개발용 하위호환). 운영에서는 Railway에 별도 스케줄 3개(ingest 좌,
    ingest 우, postprocess)로 나눠 등록해서 한 프로세스가 다 떠안다가
    시간/메모리 초과로 죽는 문제(2026-08-01 실제 발생, 우측 소스 전부
    미도달)를 피한다. CRAWL_SIDE(left/right)는 CRAWL_MODE=ingest일 때만
    사용 — 미설정이면 그 모드에서 활성 소스 전부를 대상으로 한다."""
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    mode = os.environ.get("CRAWL_MODE")
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        api_key = os.environ.get("OPENROUTER_API_KEY")

        if mode != "postprocess":
            side = os.environ.get("CRAWL_SIDE")
            source_repo = PsycopgSourceRepository(conn)
            sources = source_repo.find_enabled()
            if side:
                sources = [s for s in sources if s.side == side]
            post_repo = PsycopgPostRepository(conn)
            # 좌우 피드는 제목+원문 링크만 보여주기로 함(AI 요약 생략) —
            # 등록된 소스가 전부 HTML 스크래핑이라 entry.summary가 항상
            # 비어있어(html_parsers.py) 요약 LLM 호출을 걸어도 어차피
            # summarizer=None 폴백과 결과가 같았고(둘 다 빈 문자열),
            # 요약 호출을 아예 빼면 글 하나당 LLM 호출이 절반으로 줄어
            # 수집 사이클이 빨라지고 비용도 아낌.
            political_classifier = build_post_political_classifier(api_key, model=settings.synthesis_model)
            run_ingest_cycle(
                sources,
                settings,
                post_repo,
                source_repo,
                political_classifier=political_classifier,
            )

        if mode != "ingest":
            matching_repo = PsycopgMatchingRepository(conn)
            embedder = SentenceTransformerEmbeddingProvider()
            topic_synthesizer = build_topic_synthesizer(api_key, model=settings.synthesis_model)
            run_postprocess_cycle(settings, matching_repo, embedder, topic_synthesizer=topic_synthesizer)


if __name__ == "__main__":
    main()
