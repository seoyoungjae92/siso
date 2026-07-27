import re

# backend/.../moderation/ProfanityFilter.java와 동일한 목록 — 댓글에 쓰는
# 사전 기반 1차 필터를 크롤링 게시글에도 그대로 재사용한다. LLM 순화가
# 실패(API 장애·레이트리밋)해도 항상 도는 최후 방어선이라, 두 목록이
# 어긋나지 않도록 백엔드 쪽을 고치면 이쪽도 같이 고칠 것.
BANNED_WORDS = frozenset(
    {
        "씨발", "씨팔", "시발", "개새끼", "새끼", "병신", "지랄",
        "좆", "존나", "닥쳐", "미친놈", "미친년", "걸레", "창녀",
        "죽여버려", "꺼져", "쓰레기같은", "찐따", "한남", "김치녀", "맘충",
    }
)

_WHITESPACE_RE = re.compile(r"\s+")


def contains_banned_word(text: str) -> bool:
    normalized = _WHITESPACE_RE.sub("", text)
    return any(word in normalized for word in BANNED_WORDS)
