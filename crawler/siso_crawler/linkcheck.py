from __future__ import annotations

import logging
import os
import time
from urllib.parse import urlparse

import psycopg

from .fetch import USER_AGENT, CrawlNotAllowed
from .fetch import check_dead_link as _check_dead_link
from .fetch import fetch_robots_parser as _fetch_robots_parser
from .matching_repository import MatchingRepository, PsycopgMatchingRepository
from .settings_repository import PsycopgSettingsRepository

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def scan_dead_links(
    repo: MatchingRepository,
    display_window_days: int,
    limit: int,
    fetch_robots_parser=_fetch_robots_parser,
    check_dead_link=_check_dead_link,
    sleep=time.sleep,
) -> int:
    """노출 기간 안의 글 중 원문 링크가 확실히 죽은(404/410) 것만 삭제.

    robots.txt 확인 불가/불허, 네트워크 에러, 그 외 상태 코드는 전부
    "삭제 보류"로 다음 배치에 다시 확인한다 — 일시적 장애를 삭제로
    오판하지 않기 위함(CLAUDE.md 원칙).

    robots.txt는 도메인당 한 번만 가져와 재사용한다 — 같은 사이트 게시글을
    수백 건 검사해도 매번 다시 받지 않는다(실측: 소스 몇 개만 등록해도
    이 단계가 사이트당 게시글 수만큼 매번 robots.txt를 새로 받으면서
    배치 전체가 수십 분씩 걸리는 문제를 확인함). 요청 간격 sleep도 같은
    도메인으로 가는 요청 "사이"에서만 적용한다 — 정중해야 할 대상은 각
    origin 서버지 배치 실행 전체가 아니라서, 다른 도메인으로 넘어갈 때는
    기다릴 필요가 없다."""
    deleted = 0
    parsed_by_domain: dict[str, tuple | None] = {}

    for post_id, origin_url in repo.find_link_check_candidates(display_window_days, limit):
        domain = urlparse(origin_url).netloc
        is_first_request_for_domain = domain not in parsed_by_domain

        if is_first_request_for_domain:
            try:
                parsed_by_domain[domain] = fetch_robots_parser(origin_url)
            except CrawlNotAllowed as exc:
                logger.warning("데드링크 확인 보류(robots.txt): %s — %s", origin_url, exc)
                parsed_by_domain[domain] = None

        entry = parsed_by_domain[domain]
        if entry is None:
            continue
        parser, min_interval = entry

        if not parser.can_fetch(USER_AGENT, origin_url):
            logger.warning("데드링크 확인 보류(robots.txt 불허): %s", origin_url)
            continue

        if not is_first_request_for_domain:
            sleep(min_interval)

        if check_dead_link(origin_url) and repo.delete_post(post_id):
            deleted += 1
            logger.info("데드링크 삭제: post_id=%d url=%s", post_id, origin_url)

    return deleted


def main() -> None:
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        matching_repo = PsycopgMatchingRepository(conn)
        deleted = scan_dead_links(
            matching_repo,
            display_window_days=settings.display_window_days,
            limit=settings.dead_link_scan_limit,
        )
        logger.info("데드링크 정리: %d건 삭제", deleted)


if __name__ == "__main__":
    main()
