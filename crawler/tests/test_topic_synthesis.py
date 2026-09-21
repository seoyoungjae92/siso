from siso_crawler.llm_client import SynthesizedTopic
from siso_crawler.topic_synthesis import synthesize_pending_topics

from .fakes import FakeMatchingRepository, FakeTopicSynthesizer


def test_synthesize_pending_topics_updates_pair_on_success():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌 제목", "좌 요약")], [("우 제목", "우 요약")])]
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
            (1, [("실패 좌", "요약")], [("실패 우", "요약")]),
            (2, [("좌 제목", "좌 요약")], [("우 제목", "우 요약")]),
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
            (1, [("좌1", "요약")], [("우1", "요약")]),
            (2, [("좌2", "요약")], [("우2", "요약")]),
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


def test_synthesize_pending_topics_passes_full_cohort_to_synthesizer():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[
            (1, [("좌1", "요약1"), ("좌2", "요약2")], [("우1", "요약1")]),
        ]
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t", "l", "r")})

    synthesized = synthesize_pending_topics(repo, synthesizer)

    assert synthesized == 1
    assert repo.synthesized_pairs == [(1, "t", "l", "r")]


def test_synthesize_pending_topics_passes_enrichment_to_repo():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌 제목", "좌 요약")], [("우 제목", "우 요약")])]
    )
    topic = SynthesizedTopic(
        "합성 제목",
        "좌 입장",
        "우 입장",
        background="배경",
        left_points=("좌1", "좌2"),
        right_points=("우1", "우2"),
        discussion_questions=("질문1", "질문2"),
    )
    synthesizer = FakeTopicSynthesizer(results={("좌 제목", "우 제목"): topic})

    synthesize_pending_topics(repo, synthesizer)

    assert repo.synthesized_enrichments[1] == ("배경", ("좌1", "좌2"), ("우1", "우2"), ("질문1", "질문2"))


def test_synthesize_pending_topics_stops_at_daily_cap():
    # 임계값만으로는 뉴스 상황에 따라 하루 생성량이 출렁여서(2026-09-21 실측:
    # 하루 1~2건을 노린 설정에서 8건) 상한으로 못 박는다. 오늘 이미 1건을
    # 만들었고 상한이 2면 이번 사이클엔 1건만 합성해야 한다.
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[
            (1, [("좌1", "")], [("우1", "")]),
            (2, [("좌2", "")], [("우2", "")]),
        ],
        topics_created_today=1,
    )
    synthesizer = FakeTopicSynthesizer(
        results={
            ("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1"),
            ("좌2", "우2"): SynthesizedTopic("t2", "l2", "r2"),
        }
    )

    synthesized = synthesize_pending_topics(repo, synthesizer, max_topics_per_day=2)

    assert synthesized == 1
    assert [p[0] for p in repo.synthesized_pairs] == [1]


def test_synthesize_pending_topics_skips_entirely_when_cap_reached():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌1", "")], [("우1", "")])],
        topics_created_today=2,
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer, max_topics_per_day=2) == 0
    assert repo.synthesized_pairs == []


def test_synthesize_pending_topics_without_cap_is_unchanged():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌1", "")], [("우1", "")])],
        topics_created_today=99,
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer) == 1
