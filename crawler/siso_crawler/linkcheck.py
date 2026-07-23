from __future__ import annotations

import logging
import os
import time

import psycopg

from .fetch import CrawlNotAllowed
from .fetch import check_dead_link as _check_dead_link
from .fetch import check_robots_allowed as _check_robots_allowed
from .matching_repository import MatchingRepository, PsycopgMatchingRepository
from .settings_repository import PsycopgSettingsRepository

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def scan_dead_links(
    repo: MatchingRepository,
    display_window_days: int,
    check_robots_allowed=_check_robots_allowed,
    check_dead_link=_check_dead_link,
) -> int:
    """노출 기간 안의 글 중 원문 링크가 확실히 죽은(404/410) 것만 삭제.

    robots.txt 확인 불가/불허, 네트워크 에러, 그 외 상태 코드는 전부
    "삭제 보류"로 다음 배치에 다시 확인한다 — 일시적 장애를 삭제로
    오판하지 않기 위함(CLAUDE.md 원칙)."""
    deleted = 0

    for post_id, origin_url in repo.find_link_check_candidates(display_window_days):
        try:
            interval = check_robots_allowed(origin_url)
        except CrawlNotAllowed as exc:
            logger.warning("데드링크 확인 보류(robots.txt): %s — %s", origin_url, exc)
            continue

        if check_dead_link(origin_url) and repo.delete_post(post_id):
            deleted += 1
            logger.info("데드링크 삭제: post_id=%d url=%s", post_id, origin_url)

        time.sleep(interval)

    return deleted


def main() -> None:
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        matching_repo = PsycopgMatchingRepository(conn)
        deleted = scan_dead_links(matching_repo, display_window_days=settings.display_window_days)
        logger.info("데드링크 정리: %d건 삭제", deleted)


if __name__ == "__main__":
    main()
