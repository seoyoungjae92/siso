from __future__ import annotations

import logging
import os
import time

import psycopg

from .embedding import SentenceTransformerEmbeddingProvider
from .fetch import CrawlNotAllowed
from .fetch import check_robots_allowed as _check_robots_allowed
from .fetch import fetch_feed as _fetch_feed
from .matching import embed_pending_posts, match_pending_posts, prune_stale_candidates
from .matching_repository import PsycopgMatchingRepository
from .models import Source
from .pipeline import ingest_source
from .repository import PsycopgPostRepository
from .settings_repository import CrawlSettings, PsycopgSettingsRepository
from .sources_repository import PsycopgSourceRepository

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def run_cycle(
    sources: list[Source],
    settings: CrawlSettings,
    post_repo,
    matching_repo,
    embedder,
    check_robots_allowed=_check_robots_allowed,
    fetch_feed=_fetch_feed,
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
            continue
        except Exception as exc:  # noqa: BLE001 - 한 소스 실패로 전체 배치가 멈추면 안 됨
            logger.warning("소스 건너뜀(수집 실패): %s — %s", source.name, exc)
            continue

        result = ingest_source(source, raw_bytes, post_repo)
        logger.info(
            "%s: fetched=%d inserted=%d skipped=%d",
            source.name,
            result.fetched,
            result.inserted,
            result.skipped_duplicate,
        )

        time.sleep(min_interval)

    embedded = embed_pending_posts(matching_repo, embedder)
    logger.info("임베딩 계산: %d건", embedded)

    matched = match_pending_posts(matching_repo, threshold=settings.match_similarity_threshold)
    logger.info("매칭: %d쌍 생성", matched)

    pruned = prune_stale_candidates(
        matching_repo,
        grace_period_hours=settings.grace_period_hours,
        min_cluster_size=settings.min_cluster_size,
        prune_threshold=settings.prune_similarity_threshold,
    )
    logger.info("정리(prune): %d건 삭제", pruned)


def main() -> None:
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        sources = PsycopgSourceRepository(conn).find_enabled()
        post_repo = PsycopgPostRepository(conn)
        matching_repo = PsycopgMatchingRepository(conn)
        embedder = SentenceTransformerEmbeddingProvider()
        run_cycle(sources, settings, post_repo, matching_repo, embedder)


if __name__ == "__main__":
    main()
