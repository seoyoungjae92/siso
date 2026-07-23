from __future__ import annotations

from datetime import datetime, timedelta, timezone
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup

from .parser import RawEntry

TODAYHUMOR_BASE_URL = "https://www.todayhumor.co.kr"
DCINSIDE_BASE_URL = "https://gall.dcinside.com"

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


_PARSERS_BY_HOST = {
    "www.todayhumor.co.kr": parse_todayhumor_bestofbest,
    "gall.dcinside.com": parse_dcinside_gallery,
}


def get_html_parser(url: str):
    """소스의 feed_url(=목록 페이지 URL) 호스트를 기준으로 등록된 사이트별
    파서를 찾는다. 사이트를 하나 늘릴 때마다 위 파서 함수 하나 + 이
    딕셔너리에 한 줄만 추가하면 됨(범용 셀렉터 설정 프레임워크는 안 씀)."""
    return _PARSERS_BY_HOST.get(urlparse(url).netloc)
