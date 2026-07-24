from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Protocol

import anthropic
import pydantic

# 비용 최소화 우선(CLAUDE.md §1.4) — 매 사이클 여러 쌍을 처리하는 배치라
# 기본값은 가장 저렴한 모델로 두고, 품질이 부족하면 운영자가 env로
# 명시적으로 올리게 한다.
SYNTHESIS_MODEL = os.environ.get("ANTHROPIC_SYNTHESIS_MODEL") or "claude-haiku-4-5"

MAX_TOKENS = 1024

SYSTEM_PROMPT = """너는 한국 정치 커뮤니티 좌/우 게시글을 보고 중립적인 토론
주제를 만드는 편집자야. 아래 좌/우 게시글 제목·요약을 참고해서 토론하기
좋은 주제 제목과, 좌/우 각각의 핵심 입장을 요약해줘.

규칙:
1. 좌/우 입장 요약은 분량과 어조를 최대한 대칭으로 맞춰라. 한쪽이 더
   길거나, 더 정당해 보이거나, 더 감정적으로 서술되면 안 된다.
2. 비속어·욕설·인신공격 표현은 전부 순화하거나 제거해라.
3. 제공된 게시글 제목·요약에 없는 사실을 새로 지어내지 마라 — 어조와
   표현만 다듬고, 주장의 근거는 원문 범위를 넘지 마라.
4. 반드시 요청된 스키마의 JSON 형식으로만 답해라. 다른 텍스트를 덧붙이지
   마라."""


class SynthesizedTopicSchema(pydantic.BaseModel):
    title: str
    left_stance: str
    right_stance: str


@dataclass(frozen=True)
class SynthesizedTopic:
    title: str
    left_stance: str
    right_stance: str


class SynthesisFailed(Exception):
    """API 에러, 거부, 잘림, 빈 필드, JSON/스키마 불일치 등 모든 실패를
    이 예외 하나로 감싼다 — 호출부는 이 쌍만 건너뛰고 다음으로 진행."""


class TopicSynthesizer(Protocol):
    def synthesize(
        self, left_title: str, left_summary: str, right_title: str, right_summary: str
    ) -> SynthesizedTopic: ...


class AnthropicTopicSynthesizer:
    def __init__(self, api_key: str, model: str = SYNTHESIS_MODEL):
        self._client = anthropic.Anthropic(api_key=api_key)
        self._model = model

    def synthesize(
        self, left_title: str, left_summary: str, right_title: str, right_summary: str
    ) -> SynthesizedTopic:
        user_prompt = (
            f"[좌 게시글]\n제목: {left_title}\n요약: {left_summary}\n\n"
            f"[우 게시글]\n제목: {right_title}\n요약: {right_summary}"
        )

        try:
            response = self._client.messages.parse(
                model=self._model,
                max_tokens=MAX_TOKENS,
                system=SYSTEM_PROMPT,
                messages=[{"role": "user", "content": user_prompt}],
                output_format=SynthesizedTopicSchema,
            )
        except (anthropic.AnthropicError, pydantic.ValidationError) as exc:
            raise SynthesisFailed(f"LLM 호출/파싱 실패: {exc}") from exc

        if response.stop_reason in ("refusal", "max_tokens"):
            raise SynthesisFailed(f"LLM 응답 비정상 종료: stop_reason={response.stop_reason}")

        parsed = response.parsed_output
        if parsed is None:
            raise SynthesisFailed("LLM 응답에서 구조화된 출력을 얻지 못함")

        if not (parsed.title.strip() and parsed.left_stance.strip() and parsed.right_stance.strip()):
            raise SynthesisFailed("LLM 응답에 빈 필드가 있음")

        return SynthesizedTopic(
            title=parsed.title.strip(),
            left_stance=parsed.left_stance.strip(),
            right_stance=parsed.right_stance.strip(),
        )


def build_topic_synthesizer(api_key: str | None) -> TopicSynthesizer | None:
    if not api_key:
        return None
    return AnthropicTopicSynthesizer(api_key)
