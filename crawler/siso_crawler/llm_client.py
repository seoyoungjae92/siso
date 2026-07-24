from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Protocol

import httpx
import pydantic

OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
TIMEOUT_SECONDS = 30.0

# 실제 운영값은 crawl_settings.synthesis_model(어드민에서 조작 가능)에서
# 읽어온다 — 이건 그 값을 못 읽을 때(예: build_topic_synthesizer를 단독
# 호출하는 스크립트/테스트)를 위한 폴백 기본값일 뿐. 비용 최소화 우선
# (CLAUDE.md §1.4) — openrouter/free는 요청에 필요한 기능(여기선 구조화
# 출력)을 지원하는 무료 모델 중에서 자동으로 골라준다.
SYNTHESIS_MODEL = os.environ.get("OPENROUTER_SYNTHESIS_MODEL") or "openrouter/free"

# 무료 모델 중엔 최종 답변 전에 내부 추론(reasoning) 토큰을 상당히 쓰는
# 모델도 있어서, 짧은 응답이어도 여유 있게 잡아야 잘림(finish_reason=
# length)을 피할 수 있다. 무료 모델은 토큰 수와 무관하게 과금 안 되고
# (실측: usage.cost == 0), 이 배치는 하루 몇 번 도는 정도라 속도도
# 문제없어 — 넉넉하게 설정.
MAX_TOKENS = 4096

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

RESPONSE_JSON_SCHEMA = {
    "type": "object",
    "properties": {
        "title": {"type": "string"},
        "left_stance": {"type": "string"},
        "right_stance": {"type": "string"},
    },
    "required": ["title", "left_stance", "right_stance"],
    "additionalProperties": False,
}


class SynthesizedTopicSchema(pydantic.BaseModel):
    title: str
    left_stance: str
    right_stance: str


# 무료 라우터가 매번 다른 모델을 고르다 보니, 가끔 한글 요청에 러시아어/
# 중국어/아랍어 등이 뒤섞인 응답을 내는 모델이 걸린 적이 실제로 있었음
# (2026-07-24 실측). 문자(숫자·기호 제외) 중 한글 비율이 낮으면 깨진
# 응답으로 보고 버린다 — 영문 고유명사·숫자·기호가 섞이는 정상적인 경우는
# 통과하도록 기준을 낮게(70%) 잡음.
MIN_KOREAN_RATIO = 0.7


def _is_hangul(ch: str) -> bool:
    code = ord(ch)
    return (0xAC00 <= code <= 0xD7A3) or (0x1100 <= code <= 0x11FF) or (0x3130 <= code <= 0x318F)


def _korean_ratio(text: str) -> float:
    letters = [ch for ch in text if ch.isalpha()]
    if not letters:
        return 1.0
    return sum(1 for ch in letters if _is_hangul(ch)) / len(letters)


@dataclass(frozen=True)
class SynthesizedTopic:
    title: str
    left_stance: str
    right_stance: str


class SynthesisFailed(Exception):
    """API 에러, 비정상 종료, 잘림, 빈 필드, JSON/스키마 불일치 등 모든
    실패를 이 예외 하나로 감싼다 — 호출부는 이 쌍만 건너뛰고 다음으로
    진행."""


class TopicSynthesizer(Protocol):
    def synthesize(
        self, left_title: str, left_summary: str, right_title: str, right_summary: str
    ) -> SynthesizedTopic: ...


class OpenRouterTopicSynthesizer:
    def __init__(self, api_key: str, model: str = SYNTHESIS_MODEL):
        self._api_key = api_key
        self._model = model

    def synthesize(
        self, left_title: str, left_summary: str, right_title: str, right_summary: str
    ) -> SynthesizedTopic:
        user_prompt = (
            f"[좌 게시글]\n제목: {left_title}\n요약: {left_summary}\n\n"
            f"[우 게시글]\n제목: {right_title}\n요약: {right_summary}"
        )
        app_name = os.environ.get("APP_NAME", "siso")

        try:
            response = httpx.post(
                OPENROUTER_URL,
                timeout=TIMEOUT_SECONDS,
                headers={
                    "Authorization": f"Bearer {self._api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/seoyoungjae92/siso",
                    "X-Title": app_name,
                },
                json={
                    "model": self._model,
                    "max_tokens": MAX_TOKENS,
                    "messages": [
                        {"role": "system", "content": SYSTEM_PROMPT},
                        {"role": "user", "content": user_prompt},
                    ],
                    "response_format": {
                        "type": "json_schema",
                        "json_schema": {
                            "name": "synthesized_topic",
                            "strict": True,
                            "schema": RESPONSE_JSON_SCHEMA,
                        },
                    },
                    # response_format을 실제로 지원 안 하는 프로바이더는
                    # 기본적으로 파라미터를 조용히 무시하고 아무 형식으로나
                    # 응답해버린다(OpenRouter 문서 확인) — require_parameters로
                    # 그런 프로바이더 자체를 후보에서 빼서 스키마 불일치를 줄인다.
                    "provider": {"require_parameters": True},
                },
            )
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise SynthesisFailed(f"OpenRouter 호출 실패: {exc}") from exc

        try:
            data = response.json()
            choice = data["choices"][0]
            content = choice["message"]["content"]
            finish_reason = choice["finish_reason"]
        except (KeyError, IndexError, ValueError) as exc:
            raise SynthesisFailed(f"OpenRouter 응답 형식 이상: {exc}") from exc

        if finish_reason != "stop":
            raise SynthesisFailed(f"OpenRouter 응답 비정상 종료: finish_reason={finish_reason}")

        try:
            parsed = SynthesizedTopicSchema.model_validate_json(content)
        except pydantic.ValidationError as exc:
            raise SynthesisFailed(f"응답 JSON 파싱/스키마 검증 실패: {exc}") from exc

        if not (parsed.title.strip() and parsed.left_stance.strip() and parsed.right_stance.strip()):
            raise SynthesisFailed("응답에 빈 필드가 있음")

        combined = f"{parsed.title} {parsed.left_stance} {parsed.right_stance}"
        if _korean_ratio(combined) < MIN_KOREAN_RATIO:
            raise SynthesisFailed("응답의 한글 비율이 너무 낮음(다른 언어가 뒤섞인 응답으로 판단)")

        return SynthesizedTopic(
            title=parsed.title.strip(),
            left_stance=parsed.left_stance.strip(),
            right_stance=parsed.right_stance.strip(),
        )


def build_topic_synthesizer(api_key: str | None, model: str | None = None) -> TopicSynthesizer | None:
    if not api_key:
        return None
    return OpenRouterTopicSynthesizer(api_key, model=model or SYNTHESIS_MODEL)
