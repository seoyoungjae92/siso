from siso_crawler.matching import (
    embed_pending_posts,
    match_pending_posts,
    prune_stale_candidates,
)

from .fakes import FakeEmbeddingProvider, FakeMatchingRepository


def test_embed_pending_posts_embeds_and_stores_each_post():
    repo = FakeMatchingRepository(
        pending_embeddings=[(1, "제목1", "요약1"), (2, "제목2", "요약2")]
    )
    embedder = FakeEmbeddingProvider()

    count = embed_pending_posts(repo, embedder)

    assert count == 2
    assert embedder.embedded_texts == ["제목1 요약1", "제목2 요약2"]
    assert 1 in repo.updated_embeddings
    assert 2 in repo.updated_embeddings


def test_match_pending_posts_creates_pair_above_threshold():
    repo = FakeMatchingRepository(
        unmatched_left=[10],
        best_matches={10: (20, 0.8)},
    )

    matched = match_pending_posts(repo, threshold=0.6)

    assert matched == 1
    assert repo.created_pairs == [(10, 20, 0.8)]


def test_match_pending_posts_skips_below_threshold():
    repo = FakeMatchingRepository(
        unmatched_left=[10],
        best_matches={10: (20, 0.4)},
    )

    matched = match_pending_posts(repo, threshold=0.6)

    assert matched == 0
    assert repo.created_pairs == []


def test_match_pending_posts_skips_when_no_candidate():
    repo = FakeMatchingRepository(unmatched_left=[10], best_matches={})

    matched = match_pending_posts(repo, threshold=0.6)

    assert matched == 0
    assert repo.created_pairs == []


def test_prune_stale_candidates_deletes_below_min_cluster_size():
    # count_similar_posts는 자기 자신 제외 개수 — 자기 포함 3개가 되려면
    # 2개가 있어야 함. 여기선 1개(자기 포함 2개)라 min_cluster_size=3
    # 미달로 삭제 대상.
    repo = FakeMatchingRepository(prunable_posts=[1], similar_counts={1: 1})

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3)

    assert deleted == 1
    assert repo.deleted_posts == [1]


def test_prune_stale_candidates_keeps_post_at_min_cluster_size():
    # 자기 제외 2개 + 자기 자신 1개 = 자기 포함 3개, min_cluster_size=3
    # 이상이라 삭제 안 됨.
    repo = FakeMatchingRepository(prunable_posts=[1], similar_counts={1: 2})

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3)

    assert deleted == 0
    assert repo.deleted_posts == []


def test_prune_stale_candidates_skips_post_that_became_undeletable():
    # find_prunable_posts 조회 시점엔 후보였지만, 삭제 시점에 댓글/매칭이
    # 막 생겨서 delete_post가 실패(0행)한 경우 — 카운트에 안 잡혀야 함.
    repo = FakeMatchingRepository(
        prunable_posts=[1, 2],
        similar_counts={1: 0, 2: 0},
        undeletable_posts={1},
    )

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3)

    assert deleted == 1
    assert repo.deleted_posts == [2]
