import httpx
import pytest

from siso_crawler.fetch import (
    DEFAULT_MIN_INTERVAL_SECONDS,
    USER_AGENT,
    CrawlNotAllowed,
    check_dead_link,
    check_robots_allowed,
    fetch_feed,
)


def _robots_response(body: str, status_code: int = 200) -> httpx.Response:
    request = httpx.Request("GET", "https://example-community.test/robots.txt")
    return httpx.Response(status_code, text=body, request=request)


def test_check_robots_allowed_returns_default_interval_when_allowed(monkeypatch):
    monkeypatch.setattr(
        httpx, "get", lambda *a, **k: _robots_response("User-agent: *\nAllow: /")
    )

    interval = check_robots_allowed("https://example-community.test/rss")

    assert interval == DEFAULT_MIN_INTERVAL_SECONDS


def test_check_robots_allowed_raises_when_disallowed(monkeypatch):
    monkeypatch.setattr(
        httpx,
        "get",
        lambda *a, **k: _robots_response("User-agent: *\nDisallow: /rss"),
    )

    with pytest.raises(CrawlNotAllowed):
        check_robots_allowed("https://example-community.test/rss")


def test_check_robots_allowed_uses_crawl_delay_if_higher(monkeypatch):
    monkeypatch.setattr(
        httpx,
        "get",
        lambda *a, **k: _robots_response("User-agent: *\nAllow: /\nCrawl-delay: 30"),
    )

    interval = check_robots_allowed("https://example-community.test/rss")

    assert interval == 30.0


def test_check_robots_allowed_fails_closed_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "get", raise_error)

    with pytest.raises(CrawlNotAllowed):
        check_robots_allowed("https://example-community.test/rss")


def test_check_robots_allowed_allows_when_robots_txt_missing(monkeypatch):
    # robots.txt가 아예 없는(404) 사이트는 업계 관례상 제한 없음으로 처리
    # — 404를 fail-closed로 취급하면 robots.txt를 안 둔(흔한) 사이트가
    # 영구적으로 전부 막힘(더쿠 실제 사례).
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _robots_response("", status_code=404))

    interval = check_robots_allowed("https://example-community.test/rss")

    assert interval == DEFAULT_MIN_INTERVAL_SECONDS


def test_check_robots_allowed_fails_closed_on_non_404_http_error(monkeypatch):
    # 404 외 4xx/5xx(예: 403 차단)는 여전히 fail-closed 유지.
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _robots_response("", status_code=403))

    with pytest.raises(CrawlNotAllowed):
        check_robots_allowed("https://example-community.test/rss")


def test_fetch_feed_sends_user_agent_and_returns_content(monkeypatch):
    captured = {}

    def fake_get(url, timeout, follow_redirects, headers):
        captured["headers"] = headers
        request = httpx.Request("GET", url)
        return httpx.Response(200, content=b"<rss></rss>", request=request)

    monkeypatch.setattr(httpx, "get", fake_get)

    content = fetch_feed("https://example-community.test/rss")

    assert content == b"<rss></rss>"
    assert captured["headers"]["User-Agent"] == USER_AGENT


def _head_response(status_code: int) -> httpx.Response:
    request = httpx.Request("HEAD", "https://example-community.test/post/1")
    return httpx.Response(status_code, request=request)


def _get_response(status_code: int) -> httpx.Response:
    request = httpx.Request("GET", "https://example-community.test/post/1")
    return httpx.Response(status_code, request=request)


def test_check_dead_link_true_on_404(monkeypatch):
    monkeypatch.setattr(httpx, "head", lambda *a, **k: _head_response(404))

    assert check_dead_link("https://example-community.test/post/1") is True


def test_check_dead_link_true_on_410(monkeypatch):
    monkeypatch.setattr(httpx, "head", lambda *a, **k: _head_response(410))

    assert check_dead_link("https://example-community.test/post/1") is True


def test_check_dead_link_false_on_200(monkeypatch):
    monkeypatch.setattr(httpx, "head", lambda *a, **k: _head_response(200))

    assert check_dead_link("https://example-community.test/post/1") is False


def test_check_dead_link_false_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "head", raise_error)

    assert check_dead_link("https://example-community.test/post/1") is False


def test_check_dead_link_falls_back_to_get_when_head_not_supported(monkeypatch):
    monkeypatch.setattr(httpx, "head", lambda *a, **k: _head_response(405))
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _get_response(404))

    assert check_dead_link("https://example-community.test/post/1") is True


def test_check_dead_link_false_when_get_fallback_errors(monkeypatch):
    monkeypatch.setattr(httpx, "head", lambda *a, **k: _head_response(405))

    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "get", raise_error)

    assert check_dead_link("https://example-community.test/post/1") is False
