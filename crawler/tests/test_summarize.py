from siso_crawler.summarize import SUMMARY_MAX_LEN, TITLE_MAX_LEN, summarize

from .fakes import FakePostSummarizer


def test_summarize_strips_html_tags():
    assert summarize("<p>hello <b>world</b></p>").summary == "hello world"


def test_summarize_truncates_to_max_len():
    long_text = "가" * 300
    result = summarize(long_text)

    assert len(result.summary) == SUMMARY_MAX_LEN


def test_summarize_collapses_whitespace():
    assert summarize("hello   \n\n  world").summary == "hello world"


def test_summarize_unescapes_and_trims_title():
    result = summarize("본문", title="  제목 &amp; 부제  ")

    assert result.title == "제목 & 부제"


def test_summarize_truncates_title_to_max_len():
    long_title = "가" * 600

    result = summarize("본문", title=long_title)

    assert len(result.title) == TITLE_MAX_LEN


def test_summarize_uses_summarizer_when_provided():
    summarizer = FakePostSummarizer()

    result = summarize("원문 요약", title="제목", summarizer=summarizer)

    assert result.title == "재작성: 제목"
    assert result.summary == "재작성: 원문 요약"
    assert summarizer.calls == [("제목", "원문 요약")]


def test_summarize_truncates_summarizer_output_to_max_len():
    summarizer = FakePostSummarizer(prefix="가" * 300)

    result = summarize("원문", summarizer=summarizer)

    assert len(result.summary) == SUMMARY_MAX_LEN


def test_summarize_falls_back_to_original_when_summarizer_fails():
    summarizer = FakePostSummarizer(fail_on={"원문 요약"})

    result = summarize("원문 요약", title="제목", summarizer=summarizer)

    assert result.title == "제목"
    assert result.summary == "원문 요약"
