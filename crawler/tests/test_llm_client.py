import httpx
import pytest

from siso_crawler.llm_client import OpenRouterTopicSynthesizer, SynthesisFailed


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


def test_synthesize_returns_topic_on_valid_response(monkeypatch):
    content = '{"title": "제목", "left_stance": "좌 입장", "right_stance": "우 입장"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")

    assert result.title == "제목"
    assert result.left_stance == "좌 입장"
    assert result.right_stance == "우 입장"


def test_synthesize_fails_on_non_stop_finish_reason(monkeypatch):
    content = '{"title": "제목", "left_stance": "좌", "right_stance": "우"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content, finish_reason="length"))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_malformed_json(monkeypatch):
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response("이건 JSON이 아님"))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_empty_field(monkeypatch):
    content = '{"title": "  ", "left_stance": "좌", "right_stance": "우"}'
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_network_error(monkeypatch):
    def raise_error(*args, **kwargs):
        raise httpx.ConnectError("connection failed")

    monkeypatch.setattr(httpx, "post", raise_error)

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_http_error_status(monkeypatch):
    monkeypatch.setattr(
        httpx, "post", lambda *a, **k: _openrouter_response("", status_code=429)
    )

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_when_response_missing_choices(monkeypatch):
    def fake_post(*args, **kwargs):
        request = httpx.Request("POST", "https://openrouter.ai/api/v1/chat/completions")
        return httpx.Response(200, json={"choices": []}, request=request)

    monkeypatch.setattr(httpx, "post", fake_post)

    with pytest.raises(SynthesisFailed):
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


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
        _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_allows_some_non_korean_characters(monkeypatch):
    content = (
        '{"title": "AI 규제 논쟁, GDPR과 한국 상황 비교",'
        ' "left_stance": "EU GDPR 수준의 강한 규제가 필요하다는 입장이다.",'
        ' "right_stance": "과도한 규제는 스타트업 성장을 막는다는 반론이다."}'
    )
    monkeypatch.setattr(httpx, "post", lambda *a, **k: _openrouter_response(content))

    result = _synthesizer().synthesize("좌제목", "좌요약", "우제목", "우요약")

    assert "GDPR" in result.title
