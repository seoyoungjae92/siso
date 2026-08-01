import httpx
import pytest

from siso_crawler.llm_client import (
    OPENROUTER_APP_TITLE,
    OpenRouterPostPoliticalClassifier,
    OpenRouterPostSummarizer,
    OpenRouterTopicSynthesizer,
    PoliticalClassificationFailed,
    SummarizationFailed,
    SynthesisFailed,
)


def _openrouter_response(content: str, finish_reason: str = "stop", status_code: int = 200) -> httpx.Response:
    request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
    body = {
        "choices": [
            {
                "message": {"content": content},
                "finish_reason": finish_reason,
            }
        ]
    }
    return httpx.Response(status_code, json=body, request=request)


def _synthesizer() -> OpenRouterTopicSynthesizer:
    return OpenRouterTopicSynthesizer(api_key="test-key")


def test_openrouter_app_title_is_ascii_safe():
    # httpx는 HTTP 헤더 값을 ascii로만 인코딩한다 — 실제로 X-Title에
    # APP_NAME 환경변수(로컬 기본값이 "시소"처럼 한글)를 그대로 넣었다가
    # UnicodeEncodeError로 운영 배치가 크래시난 적이 있음. 이 상수는
    # 그 환경변수와 무관하게 항상 ascii여야 한다.
    OPENROUTER_APP_TITLE.encode("ascii")


def test_synthesize_sends_ascii_safe_title_header(monkeypatch):
    captured = {}

    def fake_post(*args, **kwargs):
        captured["headers"] = kwargs["headers"]
        return _openrouter_response('{"title": "제목", "left_stance": "좌", "right_stance": "우"}')

    monkeypatch.setattr(httpx, "post", fake_post)
    monkeypatch.setenv("APP_NAME", "시소")

    _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])

    assert captured["headers"]["X-Title"] == OPENROUTER_APP_TITLE
    captured["headers"]["X-Title"].encode("ascii")


def test_synthesize_returns_topic_on_valid_response(monkeypatch):
    content = '{"title": "제목", "left_stance": "좌 입장", "right_stance": "우 입장"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])

    assert result.title == "제목"
    assert result.left_stance == "좌 입장"
    assert result.right_stance == "우 입장"


def test_synthesize_fails_on_non_stop_finish_reason(monkeypatch):
    content = '{"title": "제목", "left_stance": "좌", "right_stance": "우"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content, finish_reason="length"))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_on_malformed_json(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("이건 JSON이 아님"))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_on_empty_field(monkeypatch):
    content = '{"title": "  ", "left_stance": "좌", "right_stance": "우"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "post", raise_error)

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_on_http_error_status(monkeypatch):
    monkeypatch.setattr(
        httpx, "post", lambda *a, **k: _openrouter_response("", status_code=429)
    )

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_when_response_missing_choices(monkeypatch):
    def fake_post(*args, **kwargs):
        request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
        return httpx.Response(200, json={"choices": []}, request=request)

    monkeypatch.setattr(httpx, "post", fake_post)

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_fails_on_mostly_non_korean_response(monkeypatch):
    # 2026-07-24 실제로 openrouter/free 무료 라우터에서 관측된 사례 —
    # 러시아어/중국어/아랍어가 뒤섞인 응답을 낸 모델이 걸림.
    content = (
        '{"title": "предоставление новых членов ограничения vs.低質内容遮蔽",'
        ' "left_stance": "회원가입Limits 도입이 다양한 이해관계자",'
        ' "right_stance": "저질 콘텐츠ئagression 방지를 위해"}'
    )
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])


def test_synthesize_allows_some_non_korean_characters(monkeypatch):
    content = (
        '{"title": "AI 규제 논쟁, GDPR과 한국 상황 비교",'
        ' "left_stance": "EU GDPR 수준의 강한 규제가 필요하다는 입장이다.",'
        ' "right_stance": "과도한 규제는 스타트업 성장을 막는다는 반론이다."}'
    )
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])

    assert "GDPR" in result.title


def test_synthesize_numbers_multiple_posts_per_side_in_prompt(monkeypatch):
    captured = {}

    def fake_post(*args, **kwargs):
        captured["body"] = kwargs["json"]
        return _openrouter_response('{"title": "제목", "left_stance": "좌", "right_stance": "우"}')

    monkeypatch.setattr(httpx, "post", fake_post)

    _synthesizer().synthesize(
        [("좌1", "좌요약1"), ("좌2", "좌요약2")],
        [("우1", "우요약1")],
    )

    user_message = captured["body"]["messages"][1]["content"]
    assert "[1] 제목: 좌1" in user_message
    assert "[2] 제목: 좌2" in user_message
    assert "[1] 제목: 우1" in user_message


def test_synthesize_truncates_overlong_response_fields(monkeypatch):
    content = (
        '{"title": "' + "가" * 300 + '",'
        ' "left_stance": "' + "나" * 600 + '",'
        ' "right_stance": "' + "다" * 600 + '"}'
    )
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _synthesizer().synthesize([("좌제목", "좌요약")], [("우제목", "우요약")])

    assert len(result.title) == 200
    assert len(result.left_stance) == 500
    assert len(result.right_stance) == 500


def _summarizer() -> OpenRouterPostSummarizer:
    return OpenRouterPostSummarizer(api_key="test-key")


def test_summarize_returns_rewritten_title_and_summary_on_valid_response(monkeypatch):
    content = '{"title": "재작성된 제목", "summary": "재작성된 요약"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _summarizer().summarize("제목", "원문 요약")

    assert result.title == "재작성된 제목"
    assert result.summary == "재작성된 요약"


def test_summarize_fails_on_non_stop_finish_reason(monkeypatch):
    content = '{"title": "재작성된 제목", "summary": "재작성된 요약"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content, finish_reason="length"))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_malformed_json(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("이건 JSON이 아님"))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_empty_summary_field(monkeypatch):
    content = '{"title": "재작성된 제목", "summary": "   "}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_empty_title_field(monkeypatch):
    content = '{"title": "   ", "summary": "재작성된 요약"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "post", raise_error)

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_http_error_status(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("", status_code=429))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_when_response_missing_choices(monkeypatch):
    def fake_post(*args, **kwargs):
        request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
        return httpx.Response(200, json={"choices": []}, request=request)

    monkeypatch.setattr(httpx, "post", fake_post)

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def test_summarize_fails_on_mostly_non_korean_response(monkeypatch):
    content = '{"title": "제목", "summary": "предоставление новых членов低質内容遮蔽ئagression"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SummarizationFailed):
        _summarizer().summarize("제목", "원문 요약")


def _classifier() -> OpenRouterPostPoliticalClassifier:
    return OpenRouterPostPoliticalClassifier(api_key="test-key")


def test_classify_returns_true_for_political_post(monkeypatch):
    content = '{"is_political": true}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    assert _classifier().is_political("정치인 발언 논란", "요약") is True


def test_classify_returns_false_for_non_political_post(monkeypatch):
    content = '{"is_political": false}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    assert _classifier().is_political("오늘 점심 뭐 먹지", "요약") is False


def test_classify_accepts_political_key_variant(monkeypatch):
    # openrouter/free 라우팅 모델 중 일부가 요청한 스키마 키(is_political)
    # 대신 political/politics로 응답하는 경우가 실제로 관측됨(2026-08-01).
    content = '{"political": true}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    assert _classifier().is_political("정치인 발언 논란", "요약") is True


def test_classify_accepts_politics_key_variant(monkeypatch):
    content = '{"politics": false}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    assert _classifier().is_political("오늘 점심 뭐 먹지", "요약") is False


def test_classify_fails_on_non_stop_finish_reason(monkeypatch):
    content = '{"is_political": true}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content, finish_reason="length"))

    with pytest.raises(PoliticalClassificationFailed):
        _classifier().is_political("제목", "요약")


def test_classify_fails_on_malformed_json(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("이건 JSON이 아님"))

    with pytest.raises(PoliticalClassificationFailed):
        _classifier().is_political("제목", "요약")


def test_classify_fails_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "post", raise_error)

    with pytest.raises(PoliticalClassificationFailed):
        _classifier().is_political("제목", "요약")


def test_classify_fails_on_http_error_status(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("", status_code=429))

    with pytest.raises(PoliticalClassificationFailed):
        _classifier().is_political("제목", "요약")


def test_classify_fails_when_response_missing_choices(monkeypatch):
    def fake_post(*args, **kwargs):
        request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
        return httpx.Response(200, json={"choices": []}, request=request)

    monkeypatch.setattr(httpx, "post", fake_post)

    with pytest.raises(PoliticalClassificationFailed):
        _classifier().is_political("제목", "요약")
