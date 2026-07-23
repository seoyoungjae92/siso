import httpx
import pytest

from siso_crawler.fetch import (
    DEFAULT_MIN_INTERVAL_SECONDS,
    USER_AGENT,
    CrawlNotAllowed,
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


def test_check_robots_allowed_fails_closed_on_404(monkeypatch):
    monkeypatch.setattr(httpx, "get", lambda *a, **k: _robots_response("", status_code=404))

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
