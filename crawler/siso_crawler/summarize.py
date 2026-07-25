import html
import logging
import re

from .llm_client import PostSummarizer, SummarizationFailed

_TAG_RE = re.compile(r"<[^>]+>")
_WHITESPACE_RE = re.compile(r"\s+")

SUMMARY_MAX_LEN = 200

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
) -> str:
    """원문 요약을 200자 이내 한국어 문장으로 만든다.

    summarizer가 있으면 LLM으로 재작성한다 — 원문 표현을 그대로 잘라내는
    "발췌"보다 자체 문장으로 재구성하는 "재작성"이 저작권 리스크가 낮다는
    판단(CLAUDE.md 19.3절). summarizer가 없거나 실패하면(API 에러, 잘림,
    한글 비율 미달 등) 원문을 잘라내는 방식으로 안전하게 폴백한다 —
    summary는 DB에 NOT NULL이라 실패해도 게시글 저장 자체는 막지 않는다.
    """
    cleaned = _clean(raw_html)

    if summarizer is not None:
        try:
            rewritten = summarizer.summarize(title, cleaned)
            return rewritten[:max_len]
        except SummarizationFailed as exc:
            logger.warning("요약 재작성 실패, 발췌로 폴백: %s", exc)

    return cleaned[:max_len]
