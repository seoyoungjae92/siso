from siso_crawler.llm_client import SynthesizedTopic
from siso_crawler.topic_synthesis import synthesize_pending_topics

from .fakes import FakeMatchingRepository, FakeTopicSynthesizer


def test_synthesize_pending_topics_updates_pair_on_success():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, "좌 제목", "좌 요약", "우 제목", "우 요약")]
    )
    synthesizer = FakeTopicSynthesizer(
        results={("좌 제목", "우 제목"): SynthesizedTopic("합성 제목", "좌 입장", "우 입장")}
    )

    synthesized = synthesize_pending_topics(repo, synthesizer)

    assert synthesized == 1
    assert repo.synthesized_pairs == [(1, "합성 제목", "좌 입장", "우 입장")]


def test_synthesize_pending_topics_skips_and_continues_on_failure():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[
            (1, "실패 좌", "요약", "실패 우", "요약"),
            (2, "좌 제목", "좌 요약", "우 제목", "우 요약"),
        ]
    )
    synthesizer = FakeTopicSynthesizer(
        results={("좌 제목", "우 제목"): SynthesizedTopic("합성 제목", "좌 입장", "우 입장")},
        fail_keys={("실패 좌", "실패 우")},
    )

    synthesized = synthesize_pending_topics(repo, synthesizer)

    assert synthesized == 1
    assert repo.synthesized_pairs == [(2, "합성 제목", "좌 입장", "우 입장")]


def test_synthesize_pending_topics_respects_limit():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[
            (1, "좌1", "요약", "우1", "요약"),
            (2, "좌2", "요약", "우2", "요약"),
        ]
    )
    synthesizer = FakeTopicSynthesizer(
        results={
            ("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1"),
            ("좌2", "우2"): SynthesizedTopic("t2", "l2", "r2"),
        }
    )

    synthesized = synthesize_pending_topics(repo, synthesizer, limit=1)

    assert synthesized == 1
    assert repo.synthesized_pairs == [(1, "t1", "l1", "r1")]
