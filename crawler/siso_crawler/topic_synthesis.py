from __future__ import annotations

import logging

from .llm_client import SynthesisFailed, TopicSynthesizer
from .matching_repository import MatchingRepository

logger = logging.getLogger(__name__)

DEFAULT_SYNTHESIS_LIMIT = 10


def synthesize_pending_topics(
    repo: MatchingRepository, synthesizer: TopicSynthesizer, limit: int = DEFAULT_SYNTHESIS_LIMIT
) -> int:
    synthesized = 0
    for pair_id, left_title, left_summary, right_title, right_summary in repo.find_pairs_missing_synthesis(limit):
        try:
            result = synthesizer.synthesize(left_title, left_summary, right_title, right_summary)
        except SynthesisFailed as exc:
            logger.warning("주제 합성 실패(pair_id=%d): %s", pair_id, exc)
            continue

        repo.update_pair_synthesis(pair_id, result.title, result.left_stance, result.right_stance)
        synthesized += 1

    return synthesized
