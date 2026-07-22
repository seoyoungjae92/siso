from siso_crawler.summarize import SUMMARY_MAX_LEN, summarize


def test_summarize_strips_html_tags():
    assert summarize("<p>hello <b>world</b></p>") == "hello world"


def test_summarize_truncates_to_max_len():
    long_text = "가" * 300
    result = summarize(long_text)

    assert len(result) == SUMMARY_MAX_LEN


def test_summarize_collapses_whitespace():
    assert summarize("hello   \n\n  world") == "hello world"
