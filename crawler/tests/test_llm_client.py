import anthropic
import pydantic
import pytest

from siso_crawler.llm_client import (
    AnthropicTopicSynthesizer,
    SynthesisFailed,
    SynthesizedTopicSchema,
)


class _FakeMessages:
    def __init__(self, response=None, exc=None):
        self._response = response
        self._exc = exc

    def parse(self, **kwargs):
        if self._exc is not None:
            raise self._exc
        return self._response


class _FakeClient:
    def __init__(self, response=None, exc=None):
        self.messages = _FakeMessages(response=response, exc=exc)


class _FakeResponse:
    def __init__(self, stop_reason="end_turn", parsed_output=None):
        self.stop_reason = stop_reason
        self.parsed_output = parsed_output


def _synthesizer_with(response=None, exc=None) -> AnthropicTopicSynthesizer:
    synthesizer = AnthropicTopicSynthesizer(api_key="test-key")
    synthesizer._client = _FakeClient(response=response, exc=exc)
    return synthesizer


def test_synthesize_returns_topic_on_valid_response():
    parsed = SynthesizedTopicSchema(title="제목", left_stance="좌 입장", right_stance="우 입장")
    synthesizer = _synthesizer_with(response=_FakeResponse(parsed_output=parsed))

    result = synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")

    assert result.title == "제목"
    assert result.left_stance == "좌 입장"
    assert result.right_stance == "우 입장"


def test_synthesize_fails_on_refusal():
    synthesizer = _synthesizer_with(response=_FakeResponse(stop_reason="refusal"))

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_max_tokens():
    synthesizer = _synthesizer_with(response=_FakeResponse(stop_reason="max_tokens"))

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_when_parsed_output_missing():
    synthesizer = _synthesizer_with(response=_FakeResponse(parsed_output=None))

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_empty_field():
    parsed = SynthesizedTopicSchema(title="  ", left_stance="좌 입장", right_stance="우 입장")
    synthesizer = _synthesizer_with(response=_FakeResponse(parsed_output=parsed))

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_api_error():
    synthesizer = _synthesizer_with(exc=anthropic.AnthropicError("connection failed"))

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")


def test_synthesize_fails_on_validation_error():
    try:
        SynthesizedTopicSchema.model_validate({})
    except pydantic.ValidationError as exc:
        synthesizer = _synthesizer_with(exc=exc)
    else:
        pytest.fail("expected ValidationError constructing the fixture")

    with pytest.raises(SynthesisFailed):
        synthesizer.synthesize("좌제목", "좌요약", "우제목", "우요약")
