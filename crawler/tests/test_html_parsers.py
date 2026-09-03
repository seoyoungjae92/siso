from datetime import datetime, timedelta, timezone
from pathlib import Path

from siso_crawler.html_parsers import (
    get_detail_parser,
    get_html_parser,
    parse_82cook_board,
    parse_82cook_detail,
    parse_clien_board,
    parse_clien_detail,
    parse_dcinside_detail,
    parse_dcinside_gallery,
    parse_ruliweb_board,
    parse_ruliweb_detail,
    parse_theqoo_board,
    parse_todayhumor_bestofbest,
)

KST = timezone(timedelta(hours=9))

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
    assert first.published_at == "2026-07-23T21:03:00+09:00"


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
    assert first.published_at == "2026-07-19T06:47:07+09:00"


def test_parse_clien_board_excludes_non_post_rows():
    html = (FIXTURES_DIR / "clien_park_list.html").read_bytes()

    entries = parse_clien_board(html)

    # fixture엔 광고 슬롯(hongbo) 1개 + 일반 글 2개가 있음 — 광고는 제외되어야 함
    assert len(entries) == 2
    first = entries[0]
    assert first.title == "당대표는 결국 당원이 뽑습니다."
    assert first.link == "https://www.clien.net/service/board/park/19235154?od=T31&po=0&category=0&groupCd="
    assert first.summary == ""
    assert first.published_at == "2026-07-26T17:13:43+09:00"


def test_parse_82cook_board_excludes_notice_rows():
    html = (FIXTURES_DIR / "82cook_entiz_list.html").read_bytes()

    entries = parse_82cook_board(html)

    # fixture엔 공지 1개 + 일반 글 2개가 있음 — 공지는 제외되어야 함
    assert len(entries) == 2
    assert all("비밀번호 변경 공지" != e.title for e in entries)

    first = entries[0]
    assert first.title == "수리비요"
    assert first.link == "https://www.82cook.com/entiz/read.php?bn=15&num=4219150&page=1"
    assert first.summary == ""
    assert first.published_at == "2026-07-26T17:12:13+09:00"


def test_parse_ruliweb_board_excludes_notice_and_ad_rows():
    html = (FIXTURES_DIR / "ruliweb_board_300148_list.html").read_bytes()

    entries = parse_ruliweb_board(html)

    # fixture엔 공지 1개 + 광고 iframe 슬롯 1개 + 일반 글 1개가 있음
    assert len(entries) == 1
    first = entries[0]
    assert first.title == "리짜이밍 - 분당 산호초"
    assert first.link == "https://bbs.ruliweb.com/community/board/300148/read/38688868"
    assert first.summary == ""
    # "시:분"만 표시되는 오늘 글이라 정확한 시각 대신 오늘 날짜인지만 확인
    today = datetime.now(KST).date().isoformat()
    assert first.published_at == f"{today}T17:13:00+09:00"


def test_parse_ruliweb_board_strips_reply_count_from_title():
    # 제목 링크 안에 댓글 수가 <span class="num_reply"> (1)</span>로 같이
    # 들어있어서 그냥 get_text()하면 "제목(1)"처럼 붙어버리는 실제 버그
    # (2026-09-03 사용자 발견) — 제목만 남아야 함.
    html = (FIXTURES_DIR / "ruliweb_board_reply_count.html").read_bytes()

    entries = parse_ruliweb_board(html)

    assert len(entries) == 1
    assert entries[0].title == "MC딩동 폭행사건"


def test_parse_theqoo_board_excludes_notice_rows_and_handles_mixed_date_formats():
    html = (FIXTURES_DIR / "theqoo_politics_list.html").read_bytes()

    entries = parse_theqoo_board(html)

    # fixture엔 공지 1개 + 일반 글 2개(오늘 글 "시:분", 올해 글 "월.일")가 있음
    assert len(entries) == 2

    today_post = entries[0]
    assert today_post.title == "정청래 지지선언한 김부선…\"주저없이 '알정찍'\""
    assert today_post.link == "https://theqoo.net/square/4291905461?category=3836759081"
    now = datetime.now(KST)
    assert today_post.published_at == f"{now.date().isoformat()}T11:50:00+09:00"

    this_year_post = entries[1]
    assert this_year_post.published_at == f"{now.year}-07-25T00:00:00+09:00"


def test_get_html_parser_dispatches_by_host():
    assert get_html_parser("https://www.todayhumor.co.kr/board/list.php?table=bestofbest") is (
        parse_todayhumor_bestofbest
    )
    assert get_html_parser("https://gall.dcinside.com/mgallery/board/lists/?id=bosu") is (
        parse_dcinside_gallery
    )
    assert get_html_parser("https://www.clien.net/service/board/park") is parse_clien_board
    assert get_html_parser("https://www.82cook.com/entiz/enti.php?bn=15") is parse_82cook_board
    assert get_html_parser("https://bbs.ruliweb.com/community/board/300148") is parse_ruliweb_board
    assert get_html_parser("https://theqoo.net/square/category/3836759081") is parse_theqoo_board


def test_parse_dcinside_detail_extracts_body_text_without_scripts():
    html = (FIXTURES_DIR / "dcinside_detail.html").read_bytes()

    text = parse_dcinside_detail(html)

    assert "보유세 강화로 조세정의를 실현해야 한다는 목소리가 커지고 있다." in text
    assert "다주택자에 대한 세금 강화가 필요하다는 게 중론이다." in text
    assert "console.log" not in text
    assert "color: red" not in text


def test_parse_clien_detail_extracts_body_text_without_scripts():
    html = (FIXTURES_DIR / "clien_detail.html").read_bytes()

    text = parse_clien_detail(html)

    assert "부동산 정책 관련 새로운 소식이 전해졌다." in text
    assert "전문가들은 신중한 접근이 필요하다고 지적한다." in text
    assert "console.log" not in text
    assert "color: red" not in text


def test_parse_82cook_detail_extracts_body_text_without_scripts():
    html = (FIXTURES_DIR / "82cook_detail.html").read_bytes()

    text = parse_82cook_detail(html)

    assert "중개수수료 관련해서 답답한 경험을 공유합니다." in text
    assert "제도 개선이 필요하다는 목소리가 많습니다." in text
    assert "console.log" not in text
    assert "color: red" not in text


def test_parse_ruliweb_detail_extracts_body_text_without_scripts():
    html = (FIXTURES_DIR / "ruliweb_detail.html").read_bytes()

    text = parse_ruliweb_detail(html)

    assert "정치권 발언을 두고 여론이 갈리고 있다." in text
    assert "커뮤니티 반응도 엇갈리는 모습이다." in text
    assert "console.log" not in text
    assert "color: red" not in text


def test_get_detail_parser_dispatches_by_host():
    assert get_detail_parser("https://gall.dcinside.com/mgallery/board/view/?id=bosu&no=1") is (
        parse_dcinside_detail
    )
    assert get_detail_parser("https://www.clien.net/service/board/park/1") is parse_clien_detail
    assert get_detail_parser("https://www.82cook.com/entiz/read.php?bn=15&num=1") is parse_82cook_detail
    assert get_detail_parser("https://bbs.ruliweb.com/community/board/300148/read/1") is parse_ruliweb_detail
    assert get_detail_parser("https://theqoo.net/square/1") is None
    assert get_html_parser("https://unknown-site.test/list") is None
