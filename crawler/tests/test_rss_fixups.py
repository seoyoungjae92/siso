from siso_crawler.parser import RawEntry
from siso_crawler.rss_fixups import get_rss_fixup


def test_get_rss_fixup_returns_none_for_unknown_feed():
    assert get_rss_fixup("https://example.test/rss") is None


def test_clien_hot10_fixup_strips_bracket_stats_from_title():
    fixup = get_rss_fixup("https://feeds.feedburner.com/clien_hot10_rss")
    entry = RawEntry(
        title="선거전 공약대로 하면 됩니다.[10][3]",
        link="https://www.clien.net/service/board/park/1",
        summary="",
        published_at="Sat, 25 Jul 2026 17:46:00 GMT",
    )

    result = fixup(entry)

    assert result.title == "선거전 공약대로 하면 됩니다."


def test_clien_hot10_fixup_reinterprets_mislabeled_gmt_as_kst():
    # 실측 확인: 실제 원문 작성 시각이 raw pubDate 숫자와 동일하지만
    # KST였다(GMT 라벨이 틀림). "17:46:00 GMT"는 사실 17:46 KST이므로
    # UTC로는 08:46이어야 한다.
    fixup = get_rss_fixup("https://feeds.feedburner.com/clien_hot10_rss")
    entry = RawEntry(
        title="제목[1][0]",
        link="https://www.clien.net/service/board/park/1",
        summary="",
        published_at="Sat, 25 Jul 2026 17:46:00 GMT",
    )

    result = fixup(entry)

    assert result.published_at == "2026-07-25T08:46:00+00:00"


def test_clien_hot10_fixup_handles_missing_published_at():
    fixup = get_rss_fixup("https://feeds.feedburner.com/clien_hot10_rss")
    entry = RawEntry(title="제목", link="https://www.clien.net/x", summary="", published_at=None)

    result = fixup(entry)

    assert result.published_at is None


def test_clien_hot10_fixup_falls_back_to_none_on_unparsable_date():
    fixup = get_rss_fixup("https://feeds.feedburner.com/clien_hot10_rss")
    entry = RawEntry(title="제목", link="https://www.clien.net/x", summary="", published_at="이상한 값")

    result = fixup(entry)

    assert result.published_at is None
