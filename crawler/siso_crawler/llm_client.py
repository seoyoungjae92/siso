from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Protocol

import httpx
import pydantic

OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
TIMEOUT_SECONDS = 30.0

# OpenRouter 대시보드 표시용 식별자일 뿐이라 실제 서비스명(APP_NAME, "시소"처럼
# 한글일 수 있음)을 그대로 쓰면 안 됨 — HTTP 헤더는 ASCII만 허용해서 httpx가
# UnicodeEncodeError를 던짐(운영에서 실제로 겪은 크래시). 고정 ASCII 값 사용.
OPENROUTER_APP_TITLE = "siso"

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
주제를 만드는 편집자야.

[0단계: 법적 금지 콘텐츠 확인 — 다른 무엇보다 먼저 이것부터 확인해라]
좌/우 게시글 중 어느 하나라도 아래에 해당하는 내용을 포함하고 있다면,
좌/우가 같은 쟁점을 다루는지와 무관하게 무조건 no_clear_issue를 true로
하고 title/left_stance/right_stance는 빈 문자열("")로 반환해라. 이런
내용은 어조만 순화해서 요약에 담아도 안 된다 — 아예 주제 자체를 만들지
마라:
- 5·18민주화운동 등에 관한 특별법 등 관련 법령상 금지된 내용(5·18민주화
  운동의 발생 사실을 부정하거나, 북한군 개입설 등으로 왜곡·비방하는
  주장 등).
- 실존 인물(정치인 등)에 대한 근거 없는 명예훼손성 주장 — 사실 확인이
  안 된 범죄·비리·개인사 등을 마치 이미 확정된 사실인 것처럼 단정하는
  내용. 단, "~라는 의혹이 제기됐다"/"~라는 논란이 있다"처럼 이미
  공론화된 의혹·논란의 존재 자체를 전달하는 것은 허용된다 — 의혹이
  제기됐다는 사실과 그 의혹의 내용이 참이라고 단정하는 것을 구분해라.
  전자는 요약해도 되고, 후자만 금지 대상이다.
- 형법 제87조(내란)에 해당하는 행위(예: 위헌적 계엄 선포 등)를 정당화·
  옹호하거나 그럴 필요성·정당성을 주장하는 내용 — 형법 제90조 2항은
  내란을 "선동 또는 선전"(그 정당성을 널리 알리는 행위)한 자도 처벌
  대상으로 규정한다. 예를 들어 "국가 안보 위기 상황에서는 계엄 선포가
  정당하고 필요한 조치였다"처럼 계엄·내란이 옳았다/필요했다는 취지의
  주장이 원문에 있다면, 그 주장을 절대로 요약에 옮기지 마라. "~라는
  입장이다"/"~라는 주장이 있다"처럼 귀속시켜 전달하는 것도 여기서는
  안전하지 않다(다른 항목과 달리 예외 없음) — 주장 내용 자체를 우리
  사이트에 게시하는 것이 문제이므로, 누구의 의견인지와 무관하게 무조건
  no_clear_issue로 처리해라.

[1단계: 쟁점 판단 — 0단계를 통과했으면 이것부터 확인해라]
좌/우 게시글들이 실제로 같은 구체적인 사건·정책·발언을 다루고 있고, 그에
대해 서로 명확히 대비되는 입장·해석·평가를 갖고 있는지 확인해라. 아래 중
하나라도 해당하면 no_clear_issue를 true로, title/left_stance/right_stance는
빈 문자열("")로 반환하고 2단계로 넘어가지 마라:
- 좌/우가 서로 다른 구체적 사건·발언을 다루고 있다. "같은 인물이나 같은
  시기의 정치 상황"이라는 것만으로는 같은 쟁점이 아니다 — 예를 들어
  한쪽은 어떤 정치인의 개혁 성과를 다루고 다른 쪽은 그와 무관한 다른
  사건(계엄령 가능성 등)을 다룬다면, 같은 인물이 언급됐어도 억지로 엮지
  마라.
- 완전히 무관한 내용이다(예: 한쪽은 애니메이션 감상평, 다른 쪽은 특정
  정당 지지 이야기).
- 같은 사안을 다루더라도 좌/우가 실질적으로 같은 의견·관측·평가만 말할
  뿐 서로 대비되는 입장 차이가 없다. 예: 둘 다 "금리 인상이 환율을
  안정시킬 것"이라는 같은 관측만 하는 경우. 선거 결과·득표율처럼 단순
  사실을 전하는 글들도 마찬가지다 — 좌/우가 같은 결과를 비슷한 톤으로
  (둘 다 "근소한 승리"나 "이변"이라고만 언급) 전달할 뿐 그 이상의 다른
  해석·평가·전망이 없다면 이것도 명확한 쟁점 차이가 없는 것으로 본다.
- 한 진영에 게시글이 여러 건 주어졌는데 그 중 실제로 관련된 글이 하나도
  없다(일부만 무관하면 그 글은 무시하고 관련된 글만으로 판단해라).
판단이 애매하면 억지로 답을 지어내지 말고 no_clear_issue를 선택해라 —
답을 만들어내는 것보다 판단을 유보하는 게 항상 더 안전하다.

[2단계: 1단계를 통과했을 때만 — 아래 규칙에 따라 요약을 작성해라]
1. 좌/우 입장 요약은 분량과 어조를 최대한 대칭으로 맞춰라. 한쪽이 더
   길거나, 더 정당해 보이거나, 더 감정적으로 서술되면 안 된다.
2. 특정 집단을 향한 혐오 표현이나 노골적인 19금 이상의 단어만 순화하거나
   제거해라. 그 정도가 아닌 강한 어조나 가벼운 비속어는 원문 분위기를
   살려서 그대로 둬도 된다(단, 좌/우 어느 한쪽만 강하게 남기면 안 되고
   위 대칭 규칙은 항상 지켜라).
3. 제공된 게시글 제목·요약에 없는 사실을 새로 지어내지 마라 — 어조와
   표현만 다듬고, 주장의 근거는 원문 범위를 넘지 마라.
4. 반드시 요청된 스키마의 JSON 형식으로만 답해라. 다른 텍스트를 덧붙이지
   마라.
5. 각 진영에 게시글이 여러 건 주어질 수 있다 — 그 중 하나만 대표로 삼지
   말고, 여러 글에 공통되는/대표적인 입장을 종합해서 요약해라. 이 중
   1단계에서 무관하다고 걸러낸 글은 요약에 섞지 마라.
6. left_stance, right_stance는 각각 500자를 넘기지 마라.
7. 응답은 반드시 자연스러운 한국어 문장으로만 작성해라. 러시아어, 아랍어
   등 다른 문자 체계나 알파벳 조각이 단어 중간에 섞이면 절대 안 된다.
   오탈자·문법 오류 없이 매끄럽게 다듬어라."""

RESPONSE_JSON_SCHEMA = {
    "type": "object",
    "properties": {
        "no_clear_issue": {"type": "boolean"},
        "title": {"type": "string"},
        "left_stance": {"type": "string"},
        "right_stance": {"type": "string"},
    },
    "required": ["no_clear_issue", "title", "left_stance", "right_stance"],
    "additionalProperties": False,
}


class SynthesizedTopicSchema(pydantic.BaseModel):
    no_clear_issue: bool
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


def _has_disallowed_script(text: str) -> bool:
    """_korean_ratio는 전체 비율만 보기 때문에, 대부분 한국어인 문장에
    단어 하나만 키릴/아랍 문자로 깨져도(예: "ренстав", "ضبط") 70%
    임계값을 넘겨서 못 걸러낸 사례가 실제로 있었음(2026-08-02). 한글
    정치 요약에 나올 일이 없는 문자 체계(한글·ASCII 외 전부)가 단 한
    글자라도 섞여있으면 깨진 응답으로 본다."""
    for ch in text:
        if not ch.isalpha():
            continue
        if _is_hangul(ch) or ch.isascii():
            continue
        return True
    return False


@dataclass(frozen=True)
class SynthesizedTopic:
    title: str
    left_stance: str
    right_stance: str


class SynthesisFailed(Exception):
    """API 에러, 비정상 종료, 잘림, 빈 필드, JSON/스키마 불일치 등 모든
    실패를 이 예외 하나로 감싼다 — 호출부는 이 쌍만 건너뛰고 다음으로
    진행."""


# 합성 자체는 저렴한 모델(crawl_settings.synthesis_model, 어드민 설정)로
# 하되, 5·18 왜곡/명예훼손/내란 옹호 같은 법적 위험 콘텐츠 검사는 별도로
# 더 성능 좋은 모델로 한 번 더 확인한다 — 실측 결과 SYSTEM_PROMPT의 0단계
# 규칙만으로는 저렴한 모델(gemini-2.5-flash-lite)이 "국가 안보 위기
# 상황에서는 계엄 선포가 정당하고 필요한 조치였다" 같은 명백한 내란 옹호
# 사례를 반복적으로(3/3) 놓쳤음(2026-08-05 실측) — 같은 케이스를
# gemini-2.5-flash/claude-sonnet-5/gpt-5-mini는 전부 정확히 잡아냄. 법적
# 리스크는 비용보다 신뢰성이 우선이라 이 검사만 고정된 상위 모델을 쓰고,
# 어드민 설정(synthesis_model)으로 낮출 수 없게 분리해뒀다.
LEGAL_SAFETY_MODEL = "google/gemini-2.5-flash"

LEGAL_SAFETY_CHECK_PROMPT = """너는 한국 정치 콘텐츠의 법적 안전성을 검토하는
감사관이야. 아래 합성된 주제(제목 + 좌/우 시각 요약)를 보고 다음 세 가지 중
하나라도 해당하는지 판단해라:

1. 5·18민주화운동의 발생 사실을 부정하거나 북한군 개입설 등으로 왜곡·비방하는
   주장을 사실처럼(또는 어느 한쪽의 정당한 견해인 것처럼) 담고 있다.
2. 실존 인물(정치인 등)에 대한 근거 없는 혐의·비리·범죄 등을 이미 확정된
   사실인 것처럼 단정하고 있다 — "의혹이 제기됐다"/"주장한다"처럼 귀속되어
   전달되는 것은 해당 안 됨, 귀속 없이 단정하는 경우만 해당.
3. 형법 제87조(내란)에 해당하는 행위(위헌적 계엄 선포 등)를 정당화·옹호하거나
   그 필요성·정당성을 주장하는 내용을 담고 있다 — 이건 "~라는 입장이다"처럼
   귀속시켜 전달해도 해당됨(주장 내용 자체가 문제이므로 예외 없음).

반드시 JSON으로만 답해라. 다른 텍스트를 덧붙이지 마라."""

LEGAL_SAFETY_RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "violates": {"type": "boolean"},
        "reason": {"type": "string"},
    },
    "required": ["violates", "reason"],
    "additionalProperties": False,
}


class LegalSafetyCheckSchema(pydantic.BaseModel):
    violates: bool
    reason: str


def _check_legal_safety(api_key: str, title: str, left_stance: str, right_stance: str) -> None:
    """위반 시 SynthesisFailed를 던진다. 이 검사 자체가 실패(API 에러 등)해도
    안전하게 SynthesisFailed로 처리한다 — 검사가 안 됐는데 그냥 통과시키는
    쪽보다, 이번 사이클엔 버리고 다음 사이클에 재시도하는 쪽이 항상 안전하다."""
    user_prompt = f"제목: {title}\n[좌] {left_stance}\n[우] {right_stance}"
    try:
        response = httpx.post(
            OPENROUTER_URL,
            timeout=TIMEOUT_SECONDS,
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json",
                "HTTP-Referer": "https://github.com/seoyoungjae92/siso",
                "X-Title": OPENROUTER_APP_TITLE,
            },
            json={
                "model": LEGAL_SAFETY_MODEL,
                "max_tokens": MAX_TOKENS,
                "messages": [
                    {"role": "system", "content": LEGAL_SAFETY_CHECK_PROMPT},
                    {"role": "user", "content": user_prompt},
                ],
                "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                        "name": "legal_safety_check",
                        "strict": True,
                        "schema": LEGAL_SAFETY_RESPONSE_SCHEMA,
                    },
                },
                "provider": {"require_parameters": True},
            },
        )
        response.raise_for_status()
        data = response.json()
        choice = data["choices"][0]
        if choice["finish_reason"] != "stop":
            raise SynthesisFailed(f"법적 안전성 검사 응답 비정상 종료: {choice['finish_reason']}")
        parsed = LegalSafetyCheckSchema.model_validate_json(choice["message"]["content"])
    except SynthesisFailed:
        raise
    except (httpx.HTTPError, KeyError, IndexError, ValueError, pydantic.ValidationError) as exc:
        raise SynthesisFailed(f"법적 안전성 검사 실패: {exc}") from exc

    if parsed.violates:
        raise SynthesisFailed(f"법적 안전성 검사에서 위반 판정: {parsed.reason}")


class TopicSynthesizer(Protocol):
    def synthesize(
        self, left_posts: list[tuple[str, str]], right_posts: list[tuple[str, str]]
    ) -> SynthesizedTopic: ...


def _format_side(posts: list[tuple[str, str]]) -> str:
    return "\n\n".join(f"[{i + 1}] 제목: {title}\n요약: {summary}" for i, (title, summary) in enumerate(posts))


class OpenRouterTopicSynthesizer:
    def __init__(self, api_key: str, model: str = SYNTHESIS_MODEL):
        self._api_key = api_key
        self._model = model

    def synthesize(
        self, left_posts: list[tuple[str, str]], right_posts: list[tuple[str, str]]
    ) -> SynthesizedTopic:
        user_prompt = (
            f"[좌 게시글]\n{_format_side(left_posts)}\n\n[우 게시글]\n{_format_side(right_posts)}"
        )

        try:
            response = httpx.post(
                OPENROUTER_URL,
                timeout=TIMEOUT_SECONDS,
                headers={
                    "Authorization": f"Bearer {self._api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/seoyoungjae92/siso",
                    "X-Title": OPENROUTER_APP_TITLE,
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

        if parsed.no_clear_issue:
            raise SynthesisFailed("좌/우 게시글 사이에 명확한 공통 쟁점을 찾지 못함(모델 판단)")

        if not (parsed.title.strip() and parsed.left_stance.strip() and parsed.right_stance.strip()):
            raise SynthesisFailed("응답에 빈 필드가 있음")

        combined = f"{parsed.title} {parsed.left_stance} {parsed.right_stance}"
        if _korean_ratio(combined) < MIN_KOREAN_RATIO:
            raise SynthesisFailed("응답의 한글 비율이 너무 낮음(다른 언어가 뒤섞인 응답으로 판단)")

        if _has_disallowed_script(combined):
            raise SynthesisFailed("응답에 한글/영문 외 문자 체계(러시아어·아랍어 등)가 섞여있음")

        title = parsed.title.strip()[:200]
        left_stance = parsed.left_stance.strip()[:500]
        right_stance = parsed.right_stance.strip()[:500]

        _check_legal_safety(self._api_key, title, left_stance, right_stance)

        return SynthesizedTopic(title=title, left_stance=left_stance, right_stance=right_stance)


def build_topic_synthesizer(api_key: str | None, model: str | None = None) -> TopicSynthesizer | None:
    if not api_key:
        return None
    return OpenRouterTopicSynthesizer(api_key, model=model or SYNTHESIS_MODEL)


SUMMARIZE_SYSTEM_PROMPT = """너는 커뮤니티 게시글 제목과 요약을 다시 쓰는
편집자야. 아래 원문 제목·요약을 참고해서 같은 사실관계를 담은 새로운
한국어 제목과 요약을 직접 작성해줘.

규칙:
1. 원문 문장을 그대로 베끼지 말고 반드시 다른 표현으로 다시 써라.
2. 제목은 100자, 요약은 200자를 넘기지 마라.
3. 원문에 없는 사실을 새로 지어내지 마라 — 사실관계는 원문 범위를
   넘지 마라.
4. 특정 집단을 향한 혐오 표현이나 노골적인 19금 이상의 단어만 순화하거나
   제거해라. 그 정도가 아닌 강한 어조나 가벼운 비속어는 원문 분위기를
   살려서 그대로 둬도 된다.
5. 반드시 요청된 스키마의 JSON 형식으로만 답해라. 다른 텍스트를 덧붙이지
   마라."""

SUMMARIZE_RESPONSE_JSON_SCHEMA = {
    "type": "object",
    "properties": {"title": {"type": "string"}, "summary": {"type": "string"}},
    "required": ["title", "summary"],
    "additionalProperties": False,
}


class SummarizedPostSchema(pydantic.BaseModel):
    title: str
    summary: str


@dataclass(frozen=True)
class SummarizedPost:
    title: str
    summary: str


class SummarizationFailed(Exception):
    """API 에러, 비정상 종료, 잘림, 빈 필드, JSON/스키마 불일치, 한글 비율
    미달 등 모든 실패를 이 예외 하나로 감싼다 — 호출부는 원문 제목·발췌
    요약으로 안전하게 폴백한다."""


class PostSummarizer(Protocol):
    def summarize(self, title: str, raw_summary: str) -> SummarizedPost: ...


class OpenRouterPostSummarizer:
    def __init__(self, api_key: str, model: str = SYNTHESIS_MODEL):
        self._api_key = api_key
        self._model = model

    def summarize(self, title: str, raw_summary: str) -> SummarizedPost:
        user_prompt = f"제목: {title}\n원문 요약: {raw_summary}"

        try:
            response = httpx.post(
                OPENROUTER_URL,
                timeout=TIMEOUT_SECONDS,
                headers={
                    "Authorization": f"Bearer {self._api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/seoyoungjae92/siso",
                    "X-Title": OPENROUTER_APP_TITLE,
                },
                json={
                    "model": self._model,
                    "max_tokens": MAX_TOKENS,
                    "messages": [
                        {"role": "system", "content": SUMMARIZE_SYSTEM_PROMPT},
                        {"role": "user", "content": user_prompt},
                    ],
                    "response_format": {
                        "type": "json_schema",
                        "json_schema": {
                            "name": "summarized_post",
                            "strict": True,
                            "schema": SUMMARIZE_RESPONSE_JSON_SCHEMA,
                        },
                    },
                    "provider": {"require_parameters": True},
                },
            )
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise SummarizationFailed(f"OpenRouter 호출 실패: {exc}") from exc

        try:
            data = response.json()
            choice = data["choices"][0]
            content = choice["message"]["content"]
            finish_reason = choice["finish_reason"]
        except (KeyError, IndexError, ValueError) as exc:
            raise SummarizationFailed(f"OpenRouter 응답 형식 이상: {exc}") from exc

        if finish_reason != "stop":
            raise SummarizationFailed(f"OpenRouter 응답 비정상 종료: finish_reason={finish_reason}")

        try:
            parsed = SummarizedPostSchema.model_validate_json(content)
        except pydantic.ValidationError as exc:
            raise SummarizationFailed(f"응답 JSON 파싱/스키마 검증 실패: {exc}") from exc

        new_title = parsed.title.strip()
        new_summary = parsed.summary.strip()
        if not (new_title and new_summary):
            raise SummarizationFailed("응답에 빈 필드가 있음")

        if _korean_ratio(f"{new_title} {new_summary}") < MIN_KOREAN_RATIO:
            raise SummarizationFailed("응답의 한글 비율이 너무 낮음(다른 언어가 뒤섞인 응답으로 판단)")

        return SummarizedPost(title=new_title, summary=new_summary)


def build_post_summarizer(api_key: str | None, model: str | None = None) -> PostSummarizer | None:
    if not api_key:
        return None
    return OpenRouterPostSummarizer(api_key, model=model or SYNTHESIS_MODEL)


POLITICAL_CLASSIFY_SYSTEM_PROMPT = """너는 한국 커뮤니티 게시글이 정치·시사 관련
내용인지 판별하는 분류기야. 아래 제목과 요약을 보고 정치인, 정당, 선거, 정부
정책, 시사 이슈에 대한 의견이나 논쟁을 다루는 글인지 판단해줘.

규칙:
1. 요리, 여행, IT 제품 후기, 스포츠, 연예인 잡담 등 정치와 무관한 일상 글은
   정치 아님으로 분류해라.
2. 정치인·정당·정부·선거·시사 이슈를 언급하거나 논쟁하는 글은 정치로
   분류해라.
3. 애매하면(예: 정치인이 언급됐지만 핵심은 다른 주제) 정치로 분류해라 —
   걸러내는 쪽보다 남겨두는 쪽이 안전하다.
4. 반드시 요청된 스키마의 JSON 형식으로만 답해라. 다른 텍스트를 덧붙이지
   마라."""

POLITICAL_CLASSIFY_RESPONSE_JSON_SCHEMA = {
    "type": "object",
    "properties": {"is_political": {"type": "boolean"}},
    "required": ["is_political"],
    "additionalProperties": False,
}


class PoliticalClassificationSchema(pydantic.BaseModel):
    # openrouter/free는 매 호출마다 다른 모델로 라우팅되는데, 일부 모델이
    # 요청한 스키마의 키 이름(is_political)을 안 지키고 political/politics로
    # 응답하는 경우가 실제로 잦음(2026-08-01 운영 로그로 확인) — 스키마
    # 검증 실패로 매번 버려지면 정치성 필터가 사실상 안 걸러지는 것과
    # 같아서, 흔한 변형 키 이름도 같은 필드로 인정한다.
    is_political: bool = pydantic.Field(validation_alias=pydantic.AliasChoices("is_political", "political", "politics"))


class PoliticalClassificationFailed(Exception):
    """API 에러, 비정상 종료, 잘림, JSON/스키마 불일치 등 모든 실패를 이
    예외 하나로 감싼다 — 호출부는 판단 실패로 보고 안전하게(글을 남겨두는
    쪽으로) 폴백한다."""


class PostPoliticalClassifier(Protocol):
    def is_political(self, title: str, summary: str) -> bool: ...


class OpenRouterPostPoliticalClassifier:
    def __init__(self, api_key: str, model: str = SYNTHESIS_MODEL):
        self._api_key = api_key
        self._model = model

    def is_political(self, title: str, summary: str) -> bool:
        user_prompt = f"제목: {title}\n요약: {summary}"

        try:
            response = httpx.post(
                OPENROUTER_URL,
                timeout=TIMEOUT_SECONDS,
                headers={
                    "Authorization": f"Bearer {self._api_key}",
                    "Content-Type": "application/json",
                    "HTTP-Referer": "https://github.com/seoyoungjae92/siso",
                    "X-Title": OPENROUTER_APP_TITLE,
                },
                json={
                    "model": self._model,
                    "max_tokens": MAX_TOKENS,
                    "messages": [
                        {"role": "system", "content": POLITICAL_CLASSIFY_SYSTEM_PROMPT},
                        {"role": "user", "content": user_prompt},
                    ],
                    "response_format": {
                        "type": "json_schema",
                        "json_schema": {
                            "name": "political_classification",
                            "strict": True,
                            "schema": POLITICAL_CLASSIFY_RESPONSE_JSON_SCHEMA,
                        },
                    },
                    "provider": {"require_parameters": True},
                },
            )
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise PoliticalClassificationFailed(f"OpenRouter 호출 실패: {exc}") from exc

        try:
            data = response.json()
            choice = data["choices"][0]
            content = choice["message"]["content"]
            finish_reason = choice["finish_reason"]
        except (KeyError, IndexError, ValueError) as exc:
            raise PoliticalClassificationFailed(f"OpenRouter 응답 형식 이상: {exc}") from exc

        if finish_reason != "stop":
            raise PoliticalClassificationFailed(
                f"OpenRouter 응답 비정상 종료: finish_reason={finish_reason}"
            )

        try:
            parsed = PoliticalClassificationSchema.model_validate_json(content)
        except pydantic.ValidationError as exc:
            raise PoliticalClassificationFailed(f"응답 JSON 파싱/스키마 검증 실패: {exc}") from exc

        return parsed.is_political


def build_post_political_classifier(
    api_key: str | None, model: str | None = None
) -> PostPoliticalClassifier | None:
    if not api_key:
        return None
    return OpenRouterPostPoliticalClassifier(api_key, model=model or SYNTHESIS_MODEL)
