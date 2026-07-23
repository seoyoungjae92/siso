from __future__ import annotations

from urllib.parse import urljoin
from urllib.robotparser import RobotFileParser

import httpx

USER_AGENT = "siso-crawler/0.1 (+contact: TODO-set-before-launch@example.com)"
DEFAULT_MIN_INTERVAL_SECONDS = 10.0
TIMEOUT_SECONDS = 10.0


class CrawlNotAllowed(Exception):
    """robots.txt가 대상 URL 접근을 허용하지 않을 때 발생.

    robots.txt 자체를 가져올 수 없는 경우(네트워크 에러, 4xx/5xx 등)도
    "허용 확인 불가"로 보고 fail-closed 처리한다 — CLAUDE.md의
    "robots.txt 코드로 강제" 요구사항 취지에 맞춤.
    """


def check_robots_allowed(base_url: str, target_url: str) -> float:
    """target_url이 robots.txt상 허용되면 적용할 최소 요청 간격(초)을
    반환하고, 허용되지 않으면 CrawlNotAllowed를 발생시킨다."""
    robots_url = urljoin(base_url, "/robots.txt")
    try:
        response = httpx.get(
            robots_url, timeout=TIMEOUT_SECONDS, headers={"User-Agent": USER_AGENT}
        )
        response.raise_for_status()
    except httpx.HTTPError as exc:
        raise CrawlNotAllowed(f"robots.txt 확인 실패: {robots_url}") from exc

    parser = RobotFileParser()
    parser.parse(response.text.splitlines())

    if not parser.can_fetch(USER_AGENT, target_url):
        raise CrawlNotAllowed(f"robots.txt가 접근을 허용하지 않음: {target_url}")

    crawl_delay = parser.crawl_delay(USER_AGENT)
    return max(DEFAULT_MIN_INTERVAL_SECONDS, float(crawl_delay)) if crawl_delay else DEFAULT_MIN_INTERVAL_SECONDS


def fetch_feed(url: str, timeout: float = TIMEOUT_SECONDS) -> bytes:
    response = httpx.get(
        url, timeout=timeout, follow_redirects=True, headers={"User-Agent": USER_AGENT}
    )
    response.raise_for_status()
    return response.content
