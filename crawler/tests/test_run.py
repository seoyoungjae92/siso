from siso_crawler.fetch import CrawlNotAllowed
from siso_crawler.llm_client import SynthesizedTopic
from siso_crawler.models import Source
from siso_crawler.run import run_cycle, run_ingest_cycle, run_postprocess_cycle
from siso_crawler.settings_repository import CrawlSettings

from .fakes import (
    FakeEmbeddingProvider,
    FakeMatchingRepository,
    FakePostPoliticalClassifier,
    FakePostRepository,
    FakePostSummarizer,
    FakeSourceRepository,
    FakeTopicSynthesizer,
    fake_fetch_robots_parser,
)

SETTINGS = CrawlSettings(
    match_similarity_threshold=0.6,
    prune_similarity_threshold=0.5,
    min_cluster_size=3,
    grace_period_hours=48,
    display_window_days=7,
)


def source(id_, feed_url="https://example-community.test/rss"):
    return Source(
        id=id_,
        name=f"source-{id_}",
        side="left",
        base_url="https://example-community.test",
        feed_url=feed_url,
        crawl_type="rss",
        enabled=True,
    )


def test_run_cycle_ingests_each_source_and_runs_matching(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[source(1)],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert len(post_repo.inserted) == 2


def test_run_cycle_skips_source_without_feed_url(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[source(1, feed_url=None)],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert post_repo.inserted == []


def test_run_cycle_skips_source_disallowed_by_robots_and_continues(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()

    def check_robots_allowed(target_url):
        if "rss2" not in target_url:
            raise CrawlNotAllowed("disallowed")
        return 0

    run_cycle(
        sources=[source(1), source(2, feed_url="https://example-community.test/rss2")],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=check_robots_allowed,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    # source 1은 건너뛰고 source 2만 수집됨
    assert len(post_repo.inserted) == 2
    assert all(p["source_id"] == 2 for p in post_repo.inserted)


def test_run_cycle_skips_source_on_fetch_failure_and_continues(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()

    def fetch_feed(url):
        if "rss2" not in url:
            raise ConnectionError("network down")
        return sample_feed_bytes

    run_cycle(
        sources=[source(1), source(2, feed_url="https://example-community.test/rss2")],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=fetch_feed,
    )

    assert len(post_repo.inserted) == 2
    assert all(p["source_id"] == 2 for p in post_repo.inserted)


def test_run_cycle_disables_source_after_reaching_failure_threshold(sample_feed_bytes):
    # CLAUDE.md §4.2: 실패/차단 감지 시 자동 비활성화 + 관리자 알림.
    # threshold=2로 두 사이클 연속 실패시키면 두 번째 실패 시점에
    # 비활성화+알림이 기록돼야 한다.
    settings = CrawlSettings(
        match_similarity_threshold=0.6,
        prune_similarity_threshold=0.5,
        min_cluster_size=3,
        grace_period_hours=48,
        display_window_days=7,
        source_failure_threshold=2,
    )
    source_repo = FakeSourceRepository()

    for _ in range(2):
        run_cycle(
            sources=[source(1)],
            settings=settings,
            post_repo=FakePostRepository(),
            matching_repo=FakeMatchingRepository(),
            embedder=FakeEmbeddingProvider(),
            source_repo=source_repo,
            check_robots_allowed=lambda target_url: 0,
            fetch_feed=lambda url: (_ for _ in ()).throw(ConnectionError("network down")),
        )

    assert source_repo.disabled == [1]
    assert source_repo.alerts == [(1, "source-1", 2)]


def test_run_cycle_does_not_disable_source_below_failure_threshold(sample_feed_bytes):
    settings = CrawlSettings(
        match_similarity_threshold=0.6,
        prune_similarity_threshold=0.5,
        min_cluster_size=3,
        grace_period_hours=48,
        display_window_days=7,
        source_failure_threshold=2,
    )
    source_repo = FakeSourceRepository()

    run_cycle(
        sources=[source(1)],
        settings=settings,
        post_repo=FakePostRepository(),
        matching_repo=FakeMatchingRepository(),
        embedder=FakeEmbeddingProvider(),
        source_repo=source_repo,
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: (_ for _ in ()).throw(ConnectionError("network down")),
    )

    assert source_repo.disabled == []


def test_run_cycle_robots_disallowed_counts_toward_failure_threshold(sample_feed_bytes):
    # "차단"도 §4.2의 실패 감지 대상 — robots.txt가 막아도 카운트돼야 한다.
    settings = CrawlSettings(
        match_similarity_threshold=0.6,
        prune_similarity_threshold=0.5,
        min_cluster_size=3,
        grace_period_hours=48,
        display_window_days=7,
        source_failure_threshold=1,
    )
    source_repo = FakeSourceRepository()

    def check_robots_allowed(target_url):
        raise CrawlNotAllowed("disallowed")

    run_cycle(
        sources=[source(1)],
        settings=settings,
        post_repo=FakePostRepository(),
        matching_repo=FakeMatchingRepository(),
        embedder=FakeEmbeddingProvider(),
        source_repo=source_repo,
        check_robots_allowed=check_robots_allowed,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert source_repo.disabled == [1]


def test_run_cycle_success_resets_failure_count(sample_feed_bytes):
    # 실패 한 번 겪은 뒤 다음 사이클에 성공하면 카운터가 리셋돼야 한다 —
    # 안 그러면 가끔 실패하는 정상 소스도 결국 누적으로 비활성화된다.
    settings = CrawlSettings(
        match_similarity_threshold=0.6,
        prune_similarity_threshold=0.5,
        min_cluster_size=3,
        grace_period_hours=48,
        display_window_days=7,
        source_failure_threshold=2,
    )
    source_repo = FakeSourceRepository()

    run_cycle(
        sources=[source(1)],
        settings=settings,
        post_repo=FakePostRepository(),
        matching_repo=FakeMatchingRepository(),
        embedder=FakeEmbeddingProvider(),
        source_repo=source_repo,
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: (_ for _ in ()).throw(ConnectionError("network down")),
    )
    run_cycle(
        sources=[source(1)],
        settings=settings,
        post_repo=FakePostRepository(),
        matching_repo=FakeMatchingRepository(),
        embedder=FakeEmbeddingProvider(),
        source_repo=source_repo,
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert source_repo.failure_counts[1] == 0
    assert source_repo.disabled == []


def test_run_cycle_runs_embedding_and_matching_after_ingest(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        pending_embeddings=[(1, "제목", "요약")],
        unmatched_left=[10],
        best_matches={10: (20, 0.8)},
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert 1 in matching_repo.updated_embeddings
    assert matching_repo.created_pairs == [([10], [20], 0.8)]


def test_run_cycle_prunes_stale_candidates_using_settings(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        prunable_posts=[99],
        similar_counts={99: 0},  # 자기 포함 1개 < min_cluster_size(3) → 삭제 대상
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert matching_repo.deleted_posts == [99]


def test_run_cycle_runs_synthesis_when_synthesizer_provided(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌", "좌요약")], [("우", "우요약")])],
    )
    embedder = FakeEmbeddingProvider()
    synthesizer = FakeTopicSynthesizer(
        results={("좌", "우"): SynthesizedTopic("제목", "좌입장", "우입장")}
    )

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        topic_synthesizer=synthesizer,
    )

    assert matching_repo.synthesized_pairs == [(1, "제목", "좌입장", "우입장")]


def test_run_cycle_skips_synthesis_when_synthesizer_is_none(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, [("좌", "좌요약")], [("우", "우요약")])],
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert matching_repo.synthesized_pairs == []


def test_run_cycle_continues_later_stages_when_an_earlier_stage_fails(sample_feed_bytes):
    # 예전엔 매칭/정리/데드링크/합성 중 하나가 죽으면 사이클 전체가
    # 중단돼서, 그날 멀쩡히 돌 수 있었던 뒤쪽 단계까지 못 돌았음 — prune
    # 단계를 강제로 실패시켜도 그 뒤(데드링크 정리) 단계는 정상 수행돼야
    # 한다.
    class _RepoWithFailingPrune(FakeMatchingRepository):
        def find_prunable_posts(self, grace_period_hours, limit):
            raise RuntimeError("boom")

    post_repo = FakePostRepository()
    matching_repo = _RepoWithFailingPrune(
        link_check_candidates=[(1, "https://example-community.test/dead")],
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        check_dead_link=lambda url: True,
        fetch_robots_parser=fake_fetch_robots_parser,
    )

    assert matching_repo.deleted_posts == [1]


def test_run_cycle_deletes_confirmed_dead_links_using_display_window(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/dead")],
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        check_dead_link=lambda url: True,
        fetch_robots_parser=fake_fetch_robots_parser,
    )

    assert matching_repo.deleted_posts == [1]


def test_run_cycle_keeps_post_when_link_still_alive(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        link_check_candidates=[(1, "https://example-community.test/alive")],
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        check_dead_link=lambda url: False,
        fetch_robots_parser=fake_fetch_robots_parser,
    )

    assert matching_repo.deleted_posts == []


def test_run_cycle_passes_summarizer_to_ingestion(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()
    summarizer = FakePostSummarizer()

    run_cycle(
        sources=[source(1)],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        summarizer=summarizer,
    )

    assert len(summarizer.calls) == 2
    assert all(p["summary"].startswith("재작성: ") for p in post_repo.inserted)


def test_run_cycle_passes_political_classifier_to_ingestion(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository()
    embedder = FakeEmbeddingProvider()
    classifier = FakePostPoliticalClassifier(non_political={"첫 번째 테스트 게시글 제목입니다"})

    run_cycle(
        sources=[source(1)],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        political_classifier=classifier,
    )

    assert len(post_repo.inserted) == 1
    assert post_repo.inserted[0]["title"] == "두 번째 테스트 게시글"


def test_run_ingest_cycle_ingests_without_touching_postprocess(sample_feed_bytes):
    # 좌/우를 별도 프로세스로 분리해 돌릴 수 있으려면 ingest 단계가
    # matching_repo/embedder 없이도 완전히 독립적으로 동작해야 한다.
    post_repo = FakePostRepository()

    run_ingest_cycle(
        sources=[source(1)],
        settings=SETTINGS,
        post_repo=post_repo,
        source_repo=FakeSourceRepository(),
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert len(post_repo.inserted) == 2


def test_run_ingest_cycle_continues_after_one_source_ingest_crash(sample_feed_bytes):
    # fetch는 성공했는데 이후 처리(파싱/저장)에서 예외가 나는 소스가
    # 있어도 이후 소스들은 계속 처리돼야 한다 — 예전엔 ingest_source
    # 호출이 try/except 밖에 있어서, 글 하나 처리 중 예외가 나면 그
    # 사이클에서 이후 소스 전부가 스킵됐다(2026-08-14 발견).
    post_repo = FakePostRepository()
    source_repo = FakeSourceRepository()
    # crawl_type=html인데 등록된 HTML 파서가 없는 호스트라 parse_entries가
    # ValueError를 던짐(ingest_source 내부, fetch 이후 단계).
    broken = Source(
        id=1,
        name="broken-source",
        side="left",
        base_url="https://unregistered-host.test",
        feed_url="https://unregistered-host.test/list",
        crawl_type="html",
        enabled=True,
    )
    healthy = source(2)

    run_ingest_cycle(
        sources=[broken, healthy],
        settings=SETTINGS,
        post_repo=post_repo,
        source_repo=source_repo,
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert len(post_repo.inserted) == 2  # healthy 소스의 글들만 저장됨
    assert source_repo.failure_counts.get(1, 0) == 0  # fetch는 성공했으니 실패 카운트 안 늘어남


def test_run_postprocess_cycle_runs_matching_without_ingest():
    # postprocess 단계가 sources/post_repo 없이도 독립적으로 동작해야
    # ingest와 분리된 별도 스케줄로 돌릴 수 있다.
    matching_repo = FakeMatchingRepository(
        pending_embeddings=[(1, "제목", "요약")],
        unmatched_left=[10],
        best_matches={10: (20, 0.8)},
    )
    embedder = FakeEmbeddingProvider()

    run_postprocess_cycle(SETTINGS, matching_repo, embedder)

    assert 1 in matching_repo.updated_embeddings
    assert matching_repo.created_pairs == [([10], [20], 0.8)]
