import html
import logging
import re

from .llm_client import PostSummarizer, SummarizationFailed, SummarizedPost

_TAG_RE = re.compile(r"<[^>]+>")
_WHITESPACE_RE = re.compile(r"\s+")

SUMMARY_MAX_LEN = 200
TITLE_MAX_LEN = 500  # posts.title VARCHAR(500)과 동일한 안전장치

logger = logging.getLogger(__name__)


def _clean(raw_html: str) -> str:
    text = _TAG_RE.sub("", raw_html)
    text = html.unescape(text)
    return _WHITESPACE_RE.sub(" ", text).strip()


def summarize(
    raw_html: str,
    title: str = "",
    summarizer: PostSummarizer | None = None,
    max_len: int = SUMMARY_MAX_LEN,
) -> SummarizedPost:
    """원문 제목·요약을 다듬는다.

    summarizer가 있으면 LLM으로 제목·요약을 함께 재작성한다 — 원문 표현을
    그대로 잘라내는 "발췌"보다 자체 문장으로 재구성하는 "재작성"이 저작권
    리스크가 낮다는 판단(CLAUDE.md 19.3절)이고, 같은 호출에서 극단적
    혐오·19금 이상 표현만 순화한다(그 외 어조는 유지 — 크롤러 소스 정치
    성향 논의 중 확정). summarizer가 없거나 실패하면(API 에러, 잘림, 한글
    비율 미달 등) 원문을 그대로/잘라내는 방식으로 안전하게 폴백한다(필터
    없이 통과) — summary는 DB에 NOT NULL이라 실패해도 게시글 저장 자체는
    막지 않는다.
    """
    cleaned_summary = _clean(raw_html)
    cleaned_title = html.unescape(title).strip()

    if summarizer is not None:
        try:
            rewritten = summarizer.summarize(cleaned_title, cleaned_summary)
            return SummarizedPost(title=rewritten.title[:TITLE_MAX_LEN], summary=rewritten.summary[:max_len])
        except SummarizationFailed as exc:
            logger.warning("요약 재작성 실패, 원문으로 폴백: %s", exc)

    return SummarizedPost(title=cleaned_title[:TITLE_MAX_LEN], summary=cleaned_summary[:max_len])
