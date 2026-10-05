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
        topics_synthesized_today=1,
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
        topics_synthesized_today=2,
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer, max_topics_per_day=2) == 0
    assert repo.synthesized_pairs == []


def test_synthesize_pending_topics_without_cap_is_unchanged():
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌1", "")], [("우1", "")])],
        topics_synthesized_today=99,
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer) == 1


def test_daily_cap_counts_synthesis_time_not_match_time():
    # 상한에 걸린 후보는 다음 날로 넘어가므로 "며칠 전 매칭 + 오늘 합성"이
    # 정상인데, 매칭 시각으로 세면 그 주제가 오늘 예산을 안 쓴 것으로 처리돼
    # 상한이 무력화된다(2026-10-04 실측: 상한 2건인데 하루 7건 노출).
    repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌1", "")], [("우1", "")])],
        topics_synthesized_today=2,  # 오늘 합성된 2건(매칭은 며칠 전일 수 있음)
    )
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer, max_topics_per_day=2) == 0


class FakeDuplicateChecker:
    """duplicate_titles에 있는 제목이면 중복으로 판정. fail=True면 검사 자체가 실패."""

    def __init__(self, duplicate_titles: set | None = None, fail: bool = False):
        self.duplicate_titles = duplicate_titles or set()
        self.fail = fail
        self.calls: list[tuple] = []

    def check(self, topic, existing_titles):
        from siso_crawler.llm_client import DuplicateVerdict, SynthesisFailed

        self.calls.append((topic.title, tuple(existing_titles)))
        if self.fail:
            raise SynthesisFailed("검사 실패")
        if topic.title in self.duplicate_titles:
            return DuplicateVerdict(True, "기존 주제", "같은 사건")
        return DuplicateVerdict(False, "", "다른 사건")


def _repo_with_one_pending():
    repo = FakeMatchingRepository(pairs_missing_synthesis=[(1, [("좌1", "")], [("우1", "")])])
    repo.recent_topic_titles = ["기존 주제 A", "기존 주제 B"]
    return repo


def test_synthesize_skips_and_hides_duplicate_topic():
    # 임베딩 유사도만으로는 같은 사건을 못 거르는 구간이 있어(2026-10-04 실측)
    # 합성 직후 기존 주제 제목과 비교하는 LLM 게이트를 둔다.
    repo = _repo_with_one_pending()
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})
    checker = FakeDuplicateChecker(duplicate_titles={"t1"})

    assert synthesize_pending_topics(repo, synthesizer, duplicate_checker=checker) == 0
    assert repo.synthesized_pairs == []  # 저장하지 않고
    assert repo.duplicate_marked == [1]  # 다시 시도하지 않도록 숨김
    assert checker.calls == [("t1", ("기존 주제 A", "기존 주제 B"))]


def test_synthesize_saves_when_not_duplicate():
    repo = _repo_with_one_pending()
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer, duplicate_checker=FakeDuplicateChecker()) == 1
    assert [p[0] for p in repo.synthesized_pairs] == [1]
    assert repo.duplicate_marked == []


def test_synthesize_retries_later_when_duplicate_check_fails():
    # 검사가 안 된 채로 올리면 중복이 그대로 노출된다 — 이번 사이클만 건너뛰고
    # 쌍은 미합성으로 남겨 다음 사이클에 다시 시도한다(숨김 처리도 하지 않음).
    repo = _repo_with_one_pending()
    synthesizer = FakeTopicSynthesizer(results={("좌1", "우1"): SynthesizedTopic("t1", "l1", "r1")})

    assert synthesize_pending_topics(repo, synthesizer, duplicate_checker=FakeDuplicateChecker(fail=True)) == 0
    assert repo.synthesized_pairs == []
    assert repo.duplicate_marked == []
