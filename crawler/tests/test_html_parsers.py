from pathlib import Path

from siso_crawler.html_parsers import (
    get_html_parser,
    parse_dcinside_gallery,
    parse_todayhumor_bestofbest,
)

FIXTURES_DIR = Path(__file__).parent / "fixtures"


def test_parse_todayhumor_bestofbest_extracts_title_link_and_date():
    html = (FIXTURES_DIR / "todayhumor_list.html").read_bytes()

    entries = parse_todayhumor_bestofbest(html)

    assert len(entries) == 4
    first = entries[0]
    assert first.title == "정청래 유튜브 게시물업"
    assert first.link == (
        "https://www.todayhumor.co.kr/board/view.php?table=bestofbest&no=483476&s_no=483476&page=1"
    )
    assert first.summary == ""
    assert first.published_at == "2026-07-23T21:03:00"


def test_parse_dcinside_gallery_excludes_notice_rows():
    html = (FIXTURES_DIR / "dcinside_bosu_list.html").read_bytes()

    entries = parse_dcinside_gallery(html)

    # fixture엔 공지 1개 + 일반 글 3개가 있음 — 공지는 제외되어야 함
    assert len(entries) == 3
    assert all("신문고" != e.title for e in entries)

    first = entries[0]
    assert first.title == "트럼프 은퇴하면 뉴욕가서 5년 살다가"
    assert first.link == "https://gall.dcinside.com/mgallery/board/view/?id=bosu&no=124452&page=1"
    assert first.summary == ""
    assert first.published_at == "2026-07-19 06:47:07"


def test_get_html_parser_dispatches_by_host():
    assert get_html_parser("https://www.todayhumor.co.kr/board/list.php?table=bestofbest") is (
        parse_todayhumor_bestofbest
    )
    assert get_html_parser("https://gall.dcinside.com/mgallery/board/lists/?id=bosu") is (
        parse_dcinside_gallery
    )
    assert get_html_parser("https://unknown-site.test/list") is None
