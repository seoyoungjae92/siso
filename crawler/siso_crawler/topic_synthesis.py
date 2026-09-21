from __future__ import annotations

import logging

from .llm_client import SynthesisFailed, TopicSynthesizer
from .matching_repository import MatchingRepository

logger = logging.getLogger(__name__)

DEFAULT_SYNTHESIS_LIMIT = 10


def synthesize_pending_topics(
    repo: MatchingRepository,
    synthesizer: TopicSynthesizer,
    limit: int = DEFAULT_SYNTHESIS_LIMIT,
    max_topics_per_day: int | None = None,
) -> int:
    """max_topics_per_day가 주어지면 오늘(KST) 이미 만들어진 주제 수를 빼고
    남은 만큼만 합성한다 — 임계값만으로는 뉴스 상황에 따라 하루 생성량이
    크게 출렁여서(2026-09-21 실측: 하루 1~2건을 노린 설정에서 8건) 목표치를
    결정적으로 보장하지 못한다. 상한에 걸린 후보는 버리지 않고 title이 NULL인
    채 남아 다음 날 후보가 된다."""
    if max_topics_per_day is not None:
        remaining_today = max_topics_per_day - repo.count_topics_created_today()
        if remaining_today <= 0:
            logger.info("오늘 주제 생성 상한(%d건) 도달 — 합성 건너뜀", max_topics_per_day)
            return 0
        limit = min(limit, remaining_today)

    synthesized = 0
    for pair_id, left_posts, right_posts in repo.find_pairs_missing_synthesis(limit):
        try:
            result = synthesizer.synthesize(left_posts, right_posts)
        except SynthesisFailed as exc:
            logger.warning("주제 합성 실패(pair_id=%d): %s", pair_id, exc)
            continue

        repo.update_pair_synthesis(
            pair_id,
            result.title,
            result.left_stance,
            result.right_stance,
            background=result.background,
            left_points=result.left_points,
            right_points=result.right_points,
            discussion_questions=result.discussion_questions,
        )
        synthesized += 1

    return synthesized
