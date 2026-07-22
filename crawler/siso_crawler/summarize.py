import html
import re

_TAG_RE = re.compile(r"<[^>]+>")
_WHITESPACE_RE = re.compile(r"\s+")

SUMMARY_MAX_LEN = 200


def summarize(raw_html: str, max_len: int = SUMMARY_MAX_LEN) -> str:
    text = _TAG_RE.sub("", raw_html)
    text = html.unescape(text)
    text = _WHITESPACE_RE.sub(" ", text).strip()
    return text[:max_len]
