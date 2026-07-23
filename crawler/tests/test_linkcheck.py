from siso_crawler.fetch import CrawlNotAllowed
from siso_crawler.linkcheck import scan_dead_links

from .fakes import FakeMatchingRepository


def test_scan_dead_links_deletes_confirmed_dead_post():
    repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/post/1")]
    )

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        check_robots_allowed=lambda url: 0,
        check_dead_link=lambda url: True,
    )

    assert deleted == 1
    assert repo.deleted_posts == [1]


def test_scan_dead_links_keeps_post_when_link_alive():
    repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/post/1")]
    )

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        check_robots_allowed=lambda url: 0,
        check_dead_link=lambda url: False,
    )

    assert deleted == 0
    assert repo.deleted_posts == []


def test_scan_dead_links_skips_when_robots_disallows_and_continues():
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community.test/post/1"),
            (2, "https://example-community.test/post/2"),
        ]
    )

    def check_robots_allowed(url):
        if url.endswith("/1"):
            raise CrawlNotAllowed("disallowed")
        return 0

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        check_robots_allowed=check_robots_allowed,
        check_dead_link=lambda url: True,
    )

    assert deleted == 1
    assert repo.deleted_posts == [2]


def test_scan_dead_links_skips_post_that_became_undeletable():
    # 조회~삭제 사이에 댓글/매칭이 새로 생겨 delete_post가 실패(0행)한
    # 경우 — 카운트에 안 잡혀야 함(prune 로직과 동일한 안전장치).
    repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/post/1")],
        undeletable_posts={1},
    )

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        check_robots_allowed=lambda url: 0,
        check_dead_link=lambda url: True,
    )

    assert deleted == 0
    assert repo.deleted_posts == []
