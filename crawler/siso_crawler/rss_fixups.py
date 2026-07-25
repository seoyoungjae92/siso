from __future__ import annotations

import re
from datetime import datetime, timedelta, timezone

from .parser import RawEntry

# 클리앙 인기글 RSS(FeedBurner 경유)는 pubDate 끝에 "GMT"를 붙이지만 실제
# 값은 한국 시간(KST) 벽시계 숫자를 그대로 옮긴 것이다 — 실측으로 확인
# (실제 게시글 작성 시각과 raw pubDate 숫자가 동일, 9시간 밀려 저장됨).
# UTC로 오인해 그대로 저장하면 9시간 미래로 밀리므로 KST로 재해석해서
# UTC로 변환한다. 또한 title에 "[댓글수][추천수]"가 실시간으로 붙어 오는데
# 실제 게시글 제목엔 없어서 같이 제거한다.
_CLIEN_HOT10_URL = "https://feeds.feedburner.com/clien_hot10_rss"
_CLIEN_PUBDATE_FORMAT = "%a, %d %b %Y %H:%M:%S"
_TRAILING_BRACKET_STATS_RE = re.compile(r"(\[\d+\])+\s*$")
_KST = timezone(timedelta(hours=9))


def _clien_hot10_fixup(entry: RawEntry) -> RawEntry:
    title = _TRAILING_BRACKET_STATS_RE.sub("", entry.title).strip()

    published_at = entry.published_at
    if published_at is not None:
        raw = published_at.removesuffix(" GMT")
        try:
            published_at = (
                datetime.strptime(raw, _CLIEN_PUBDATE_FORMAT)
                .replace(tzinfo=_KST)
                .astimezone(timezone.utc)
                .isoformat()
            )
        except ValueError:
            published_at = None

    return RawEntry(title=title, link=entry.link, summary=entry.summary, published_at=published_at)


_FIXUPS_BY_FEED_URL = {
    _CLIEN_HOT10_URL: _clien_hot10_fixup,
}


def get_rss_fixup(feed_url: str):
    """소스의 feed_url을 기준으로 등록된 피드별 보정 함수를 찾는다. 대부분의
    RSS 피드는 보정이 필요 없어 기본값은 None(보정 없음) — 피드가 늘어날
    때마다 실제 문제를 실측으로 확인한 뒤 함수 하나 + 이 딕셔너리에 한 줄만
    추가한다(범용 보정 프레임워크는 안 씀)."""
    return _FIXUPS_BY_FEED_URL.get(feed_url)
