from siso_crawler.parser import parse_feed


def test_parse_feed_returns_all_entries(sample_feed_bytes):
    entries = parse_feed(sample_feed_bytes)

    assert len(entries) == 2
    assert entries[0].title == "첫 번째 테스트 게시글 제목입니다"
    assert entries[0].link == "https://example-community.test/posts/1"
    assert entries[1].link == "https://example-community.test/posts/2"
