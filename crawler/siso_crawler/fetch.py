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


def fetch_robots_parser(url: str) -> tuple[RobotFileParser, float]:
    """url의 도메인 기준 robots.txt를 가져와 파싱한 결과(파서, 최소 요청
    간격)를 반환한다. 같은 도메인의 여러 URL을 확인할 때(예: 데드링크
    일괄 검사) 이 결과를 재사용하면 URL마다 robots.txt를 다시 받지 않아도
    된다 — 파서의 can_fetch()는 순수 로컬 판정이라 URL별로 다시 호출해도
    네트워크 요청이 없다.

    robots.txt 자체를 가져올 수 없는 경우(네트워크 에러, 4xx/5xx 등)는
    CrawlNotAllowed로 fail-closed 처리한다 — CLAUDE.md의 "robots.txt
    코드로 강제" 요구사항 취지에 맞춤."""
    robots_url = urljoin(url, "/robots.txt")
    try:
        response = httpx.get(
            robots_url, timeout=TIMEOUT_SECONDS, headers={"User-Agent": USER_AGENT}
        )
        response.raise_for_status()
    except httpx.HTTPError as exc:
        raise CrawlNotAllowed(f"robots.txt 확인 실패: {robots_url}") from exc

    parser = RobotFileParser()
    parser.parse(response.text.splitlines())

    crawl_delay = parser.crawl_delay(USER_AGENT)
    min_interval = (
        max(DEFAULT_MIN_INTERVAL_SECONDS, float(crawl_delay)) if crawl_delay else DEFAULT_MIN_INTERVAL_SECONDS
    )
    return parser, min_interval


def check_robots_allowed(target_url: str) -> float:
    """target_url이 robots.txt상 허용되면 적용할 최소 요청 간격(초)을
    반환하고, 허용되지 않으면 CrawlNotAllowed를 발생시킨다.

    robots.txt는 항상 실제로 fetch할 target_url 자신의 도메인 기준으로
    확인해야 함 — feed_url이 소스의 base_url과 다른 도메인(예: RSS가
    feeds.feedburner.com에 있는 경우)일 수 있어서, 별도 base_url을
    받지 않고 target_url에서 직접 호스트를 뽑는다."""
    parser, min_interval = fetch_robots_parser(target_url)

    if not parser.can_fetch(USER_AGENT, target_url):
        raise CrawlNotAllowed(f"robots.txt가 접근을 허용하지 않음: {target_url}")

    return min_interval


def fetch_feed(url: str, timeout: float = TIMEOUT_SECONDS) -> bytes:
    response = httpx.get(
        url, timeout=timeout, follow_redirects=True, headers={"User-Agent": USER_AGENT}
    )
    response.raise_for_status()
    return response.content


_DEAD_STATUS_CODES = (404, 410)
_HEAD_NOT_SUPPORTED_STATUS_CODES = (405, 501)


def check_dead_link(url: str, timeout: float = TIMEOUT_SECONDS) -> bool:
    """원문 링크가 확실히 죽었다고(404/410) 판단될 때만 True를 반환한다.

    네트워크 에러·타임아웃·그 외 상태 코드는 일시적 장애일 수 있어
    "죽었다고 확인 안 됨"(False)으로 보수적으로 처리한다 — 삭제는
    호출부에서 True인 경우에만 수행."""
    try:
        response = httpx.head(
            url, timeout=timeout, follow_redirects=True, headers={"User-Agent": USER_AGENT}
        )
    except httpx.HTTPError:
        return False

    if response.status_code in _HEAD_NOT_SUPPORTED_STATUS_CODES:
        try:
            response = httpx.get(
                url, timeout=timeout, follow_redirects=True, headers={"User-Agent": USER_AGENT}
            )
        except httpx.HTTPError:
            return False

    return response.status_code in _DEAD_STATUS_CODES
