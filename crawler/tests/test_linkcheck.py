from siso_crawler.fetch import CrawlNotAllowed
from siso_crawler.linkcheck import scan_dead_links

from .fakes import FakeMatchingRepository, FakeRobotsParser, fake_fetch_robots_parser


def test_scan_dead_links_deletes_confirmed_dead_post():
    repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/post/1")]
    )

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=fake_fetch_robots_parser,
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
        limit=100,
        fetch_robots_parser=fake_fetch_robots_parser,
        check_dead_link=lambda url: False,
    )

    assert deleted == 0
    assert repo.deleted_posts == []


def test_scan_dead_links_skips_when_robots_disallows_specific_path():
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community.test/post/1"),
            (2, "https://example-community.test/post/2"),
        ]
    )

    class _DisallowsPost1(FakeRobotsParser):
        def can_fetch(self, user_agent, url):
            return not url.endswith("/1")

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=lambda url: (_DisallowsPost1(), 0),
        check_dead_link=lambda url: True,
    )

    assert deleted == 1
    assert repo.deleted_posts == [2]


def test_scan_dead_links_skips_when_robots_txt_unreachable():
    repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/post/1")]
    )

    def fetch_robots_parser(url):
        raise CrawlNotAllowed("robots.txt 확인 실패")

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=fetch_robots_parser,
        check_dead_link=lambda url: True,
    )

    assert deleted == 0
    assert repo.deleted_posts == []


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
        limit=100,
        fetch_robots_parser=fake_fetch_robots_parser,
        check_dead_link=lambda url: True,
    )

    assert deleted == 0
    assert repo.deleted_posts == []


def test_scan_dead_links_fetches_robots_once_per_domain():
    # 실측으로 확인된 버그: 도메인당 한 번이 아니라 게시글마다 robots.txt를
    # 다시 받아서, 소스 하나에 게시글이 수십~수백 건이면 배치 전체가 아주
    # 느려짐. 같은 도메인 게시글 3개를 줘도 fetch는 딱 1번만 나가야 한다.
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community.test/post/1"),
            (2, "https://example-community.test/post/2"),
            (3, "https://example-community.test/post/3"),
        ]
    )
    calls = []

    def fetch_robots_parser(url):
        calls.append(url)
        return FakeRobotsParser(), 0

    scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=fetch_robots_parser,
        check_dead_link=lambda url: False,
    )

    assert len(calls) == 1


def test_scan_dead_links_sleeps_between_same_domain_requests_but_not_before_first():
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community.test/post/1"),
            (2, "https://example-community.test/post/2"),
        ]
    )
    sleep_calls = []

    scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=lambda url: (FakeRobotsParser(), 12.5),
        check_dead_link=lambda url: False,
        sleep=sleep_calls.append,
    )

    assert sleep_calls == [12.5]


def test_scan_dead_links_respects_limit():
    # 매칭 안 되는 글은 노출 기간 내내 후보로 남아 쌓일 수 있어서(로컬에서
    # 실제로 659건까지 쌓여 한 사이클에 110분 걸리는 걸 확인함), limit을
    # 넘는 후보는 이번 사이클에서 건드리지 않아야 한다.
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community.test/post/1"),
            (2, "https://example-community.test/post/2"),
            (3, "https://example-community.test/post/3"),
        ]
    )

    deleted = scan_dead_links(
        repo,
        display_window_days=7,
        limit=2,
        fetch_robots_parser=fake_fetch_robots_parser,
        check_dead_link=lambda url: True,
    )

    assert deleted == 2
    assert repo.deleted_posts == [1, 2]


def test_scan_dead_links_does_not_sleep_when_switching_domains():
    repo = FakeMatchingRepository(
        link_check_candidates=[
            (1, "https://example-community-a.test/post/1"),
            (2, "https://example-community-b.test/post/1"),
        ]
    )
    sleep_calls = []

    scan_dead_links(
        repo,
        display_window_days=7,
        limit=100,
        fetch_robots_parser=lambda url: (FakeRobotsParser(), 10.0),
        check_dead_link=lambda url: False,
        sleep=sleep_calls.append,
    )

    assert sleep_calls == []
