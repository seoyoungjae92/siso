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
