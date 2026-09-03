from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup

from .parser import RawEntry

TODAYHUMOR_BASE_URL = "https://www.todayhumor.co.kr"
DCINSIDE_BASE_URL = "https://gall.dcinside.com"
CLIEN_BASE_URL = "https://www.clien.net"
CLUB82_BASE_URL = "https://www.82cook.com/entiz/"
RULIWEB_BASE_URL = "https://bbs.ruliweb.com/"
THEQOO_BASE_URL = "https://theqoo.net"

_TIME_ONLY_RE = re.compile(r"^\d{1,2}:\d{2}$")
_DATE_ONLY_RE = re.compile(r"^\d{4}\.\d{2}\.\d{2}$")
_MONTH_DAY_RE = re.compile(r"^\d{2}\.\d{2}$")
_TWO_DIGIT_YEAR_DATE_RE = re.compile(r"^\d{2}\.\d{2}\.\d{2}$")

# 오늘의유머/디시인사이드 둘 다 국내 커뮤니티라 목록 페이지에 찍힌 시각은
# 항상 한국 시간(KST, DST 없음) — 타임존 정보 없이 그대로 저장하면
# timestamptz 컬럼에 UTC로 오인되어 실제보다 9시간 미래로 밀려 저장된다.
KST = timezone(timedelta(hours=9))


def parse_todayhumor_bestofbest(html: bytes) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    for row in soup.select("tr.view"):
        link = row.select_one("td.subject a")
        if link is None:
            continue

        date_td = row.select_one("td.date")
        published_at = None
        if date_td is not None:
            try:
                published_at = (
                    datetime.strptime(date_td.get_text(strip=True), "%y/%m/%d %H:%M")
                    .replace(tzinfo=KST)
                    .isoformat()
                )
            except ValueError:
                published_at = None

        entries.append(
            RawEntry(
                title=link.get_text(strip=True),
                link=urljoin(TODAYHUMOR_BASE_URL, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def parse_dcinside_gallery(html: bytes) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    for row in soup.select("tr.us-post[data-no]"):
        if row.get("data-type") == "icon_notice":
            continue  # 상단 고정 공지는 매 사이클 동일하게 반복 노출되므로 제외

        link = row.select_one("td.gall_tit a")
        if link is None:
            continue

        date_td = row.select_one("td.gall_date")
        published_at = None
        if date_td is not None and date_td.has_attr("title"):
            try:
                published_at = (
                    datetime.strptime(date_td["title"], "%Y-%m-%d %H:%M:%S")
                    .replace(tzinfo=KST)
                    .isoformat()
                )
            except ValueError:
                published_at = None

        entries.append(
            RawEntry(
                title=link.get_text(strip=True),
                link=urljoin(DCINSIDE_BASE_URL, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def parse_clien_board(html: bytes) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    # 광고/공지 행("list_item hongbo", "list_item notice")은 클래스에
    # symph_row가 없어 이 셀렉터에서 자연히 제외된다(dcinside처럼 별도
    # 예외 처리 불필요).
    for row in soup.select("div.list_item.symph_row"):
        link = row.select_one("a.list_subject")
        if link is None:
            continue

        title_span = row.select_one("span.subject_fixed")
        title = title_span.get_text(strip=True) if title_span is not None else link.get_text(strip=True)

        timestamp_span = row.select_one("div.list_time span.timestamp")
        published_at = None
        if timestamp_span is not None:
            try:
                published_at = (
                    datetime.strptime(timestamp_span.get_text(strip=True), "%Y-%m-%d %H:%M:%S")
                    .replace(tzinfo=KST)
                    .isoformat()
                )
            except ValueError:
                published_at = None

        entries.append(
            RawEntry(
                title=title,
                link=urljoin(CLIEN_BASE_URL, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def parse_82cook_board(html: bytes) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    for row in soup.select("tr"):
        if "noticeList" in (row.get("class") or []):
            continue  # 상단 고정 공지 제외

        link = row.select_one("td.title a")
        if link is None:
            continue

        date_td = row.select_one("td.regdate")
        published_at = None
        if date_td is not None and date_td.has_attr("title"):
            try:
                published_at = (
                    datetime.strptime(date_td["title"], "%Y-%m-%d %H:%M:%S")
                    .replace(tzinfo=KST)
                    .isoformat()
                )
            except ValueError:
                published_at = None

        entries.append(
            RawEntry(
                title=link.get_text(strip=True),
                link=urljoin(CLUB82_BASE_URL, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def _parse_ruliweb_time(text: str) -> str | None:
    # 오늘 글은 "17:13"(시:분)만 나오고, 지난 글(주로 공지)은
    # "2017.05.12"(연.월.일)만 나옴 — title 속성 같은 정확한 타임스탬프가
    # 없어서 이 두 포맷만 다룬다. 둘 다 아니면 파싱 포기(None).
    if _TIME_ONLY_RE.match(text):
        today = datetime.now(KST).date()
        hour, minute = (int(part) for part in text.split(":"))
        return datetime(today.year, today.month, today.day, hour, minute, tzinfo=KST).isoformat()
    if _DATE_ONLY_RE.match(text):
        year, month, day = (int(part) for part in text.split("."))
        return datetime(year, month, day, tzinfo=KST).isoformat()
    return None


def parse_ruliweb_board(html: bytes) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    for row in soup.select("tr.table_body"):
        classes = row.get("class") or []
        if "notice" in classes or "list_inner" in classes:
            continue  # 상단 고정 공지 / 광고 iframe 슬롯 제외

        link = row.select_one("td.subject a.subject_link")
        if link is None:
            continue

        # 제목 링크 안에 댓글 수 "(2)"가 <span class="num_reply">로 같이
        # 들어있어서 get_text()로 그대로 뽑으면 "제목(2)"처럼 붙어버림 —
        # 제목만 남기려면 먼저 떼어내야 함.
        reply_count_span = link.select_one("span.num_reply")
        if reply_count_span is not None:
            reply_count_span.decompose()

        time_td = row.select_one("td.time")
        published_at = _parse_ruliweb_time(time_td.get_text(strip=True)) if time_td is not None else None

        entries.append(
            RawEntry(
                title=link.get_text(strip=True),
                link=urljoin(RULIWEB_BASE_URL, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def _parse_xe_style_time(text: str) -> str | None:
    # XE/Rhymix 계열 게시판 엔진(더쿠 등)은 시각 표기가 세 포맷으로 섞여
    # 나온다: "11:50"(오늘, 시:분) / "07.25"(올해, 월.일) / "24.04.09"(예전,
    # 두자리 연도.월.일). title 속성 같은 정확한 타임스탬프가 없어서 이 셋만
    # 다룬다. 셋 다 아니면 파싱 포기(None).
    now = datetime.now(KST)
    if _TIME_ONLY_RE.match(text):
        hour, minute = (int(part) for part in text.split(":"))
        return datetime(now.year, now.month, now.day, hour, minute, tzinfo=KST).isoformat()
    if _MONTH_DAY_RE.match(text):
        month, day = (int(part) for part in text.split("."))
        return datetime(now.year, month, day, tzinfo=KST).isoformat()
    if _TWO_DIGIT_YEAR_DATE_RE.match(text):
        yy, month, day = (int(part) for part in text.split("."))
        return datetime(2000 + yy, month, day, tzinfo=KST).isoformat()
    return None


def _parse_xe_style_board(html: bytes, base_url: str) -> list[RawEntry]:
    soup = BeautifulSoup(html, "html.parser")
    entries: list[RawEntry] = []

    for row in soup.select("tr"):
        if row.get("class"):
            continue  # 공지/이벤트 배너 등은 전부 class가 붙어있고, 일반 글만 class 없음

        link = row.select_one("td.title a")
        if link is None:
            continue

        time_td = row.select_one("td.time")
        published_at = _parse_xe_style_time(time_td.get_text(strip=True)) if time_td is not None else None

        entries.append(
            RawEntry(
                title=link.get_text(strip=True),
                link=urljoin(base_url, link["href"]),
                summary="",
                published_at=published_at,
            )
        )

    return entries


def parse_theqoo_board(html: bytes) -> list[RawEntry]:
    return _parse_xe_style_board(html, THEQOO_BASE_URL)


_PARSERS_BY_HOST = {
    "www.todayhumor.co.kr": parse_todayhumor_bestofbest,
    "gall.dcinside.com": parse_dcinside_gallery,
    "www.clien.net": parse_clien_board,
    "www.82cook.com": parse_82cook_board,
    "bbs.ruliweb.com": parse_ruliweb_board,
    "theqoo.net": parse_theqoo_board,
}


def get_html_parser(url: str):
    """소스의 feed_url(=목록 페이지 URL) 호스트를 기준으로 등록된 사이트별
    파서를 찾는다. 사이트를 하나 늘릴 때마다 위 파서 함수 하나 + 이
    딕셔너리에 한 줄만 추가하면 됨(범용 셀렉터 설정 프레임워크는 안 씀)."""
    return _PARSERS_BY_HOST.get(urlparse(url).netloc)


# 상세 페이지 본문 추출 — 목록 페이지 파서(위)와 별개로, 개별 글의 실제
# 내용을 가져와 요약(summarize.py)에 실어보내기 위한 것. 목록 파서가
# 없는 사이트와 마찬가지로, 상세 파서가 없는 사이트는 그냥 지금까지처럼
# 제목만으로 처리된다(폴백, 코드 안 건드림).
def _extract_text(soup_node) -> str:
    for tag in soup_node.select("script, style"):
        tag.decompose()
    return soup_node.get_text(" ", strip=True)


def parse_dcinside_detail(html: bytes) -> str:
    soup = BeautifulSoup(html, "html.parser")
    box = soup.select_one("div.write_div")
    return _extract_text(box) if box is not None else ""


def parse_clien_detail(html: bytes) -> str:
    soup = BeautifulSoup(html, "html.parser")
    box = soup.select_one("div.post_content")
    return _extract_text(box) if box is not None else ""


def parse_82cook_detail(html: bytes) -> str:
    soup = BeautifulSoup(html, "html.parser")
    box = soup.select_one("#articleBody")
    return _extract_text(box) if box is not None else ""


def parse_ruliweb_detail(html: bytes) -> str:
    soup = BeautifulSoup(html, "html.parser")
    box = soup.select_one("div.view_content")
    return _extract_text(box) if box is not None else ""


_DETAIL_PARSERS_BY_HOST = {
    "gall.dcinside.com": parse_dcinside_detail,
    "www.clien.net": parse_clien_detail,
    "www.82cook.com": parse_82cook_detail,
    "bbs.ruliweb.com": parse_ruliweb_detail,
}


def get_detail_parser(url: str):
    return _DETAIL_PARSERS_BY_HOST.get(urlparse(url).netloc)
