from siso_crawler.fetch import CrawlNotAllowed
from siso_crawler.llm_client import SynthesizedTopic
from siso_crawler.models import Source
from siso_crawler.run import run_cycle
from siso_crawler.settings_repository import CrawlSettings

from .fakes import FakeEmbeddingProvider, FakeMatchingRepository, FakePostRepository, FakeTopicSynthesizer

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
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=fetch_feed,
    )

    assert len(post_repo.inserted) == 2
    assert all(p["source_id"] == 2 for p in post_repo.inserted)


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
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert 1 in matching_repo.updated_embeddings
    assert matching_repo.created_pairs == [(10, 20, 0.8)]


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
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert matching_repo.deleted_posts == [99]


def test_run_cycle_runs_synthesis_when_synthesizer_provided(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, "좌", "좌요약", "우", "우요약")],
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
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
        topic_synthesizer=synthesizer,
    )

    assert matching_repo.synthesized_pairs == [(1, "제목", "좌입장", "우입장")]


def test_run_cycle_skips_synthesis_when_synthesizer_is_none(sample_feed_bytes):
    post_repo = FakePostRepository()
    matching_repo = FakeMatchingRepository(
        pairs_missing_synthesis=[(1, "좌", "좌요약", "우", "우요약")],
    )
    embedder = FakeEmbeddingProvider()

    run_cycle(
        sources=[],
        settings=SETTINGS,
        post_repo=post_repo,
        matching_repo=matching_repo,
        embedder=embedder,
        check_robots_allowed=lambda target_url: 0,
        fetch_feed=lambda url: sample_feed_bytes,
    )

    assert matching_repo.synthesized_pairs == []
