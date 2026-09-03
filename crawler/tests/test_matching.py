from siso_crawler.matching import (
    delete_stale_posts,
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
    assert repo.created_pairs == [([10], [20], 0.8)]


def test_match_pending_posts_defers_when_cohort_below_min_posts_per_side():
    # 시드 쌍(10, 20)은 있지만 같은 편 코호트가 없어서 min_posts_per_side=2를
    # 못 채움 — 이번 사이클엔 아무것도 생성되지 않아야 한다.
    repo = FakeMatchingRepository(
        unmatched_left=[10],
        best_matches={10: (20, 0.8)},
    )

    matched = match_pending_posts(repo, threshold=0.6, min_posts_per_side=2)

    assert matched == 0
    assert repo.created_pairs == []


def test_match_pending_posts_creates_pair_when_cohort_meets_min_posts_per_side():
    repo = FakeMatchingRepository(
        unmatched_left=[10],
        best_matches={10: (20, 0.8)},
        same_side_similar={10: [(11, 0.7)], 20: [(21, 0.6)]},
    )

    matched = match_pending_posts(repo, threshold=0.6, min_posts_per_side=2)

    assert matched == 1
    assert repo.created_pairs == [([10, 11], [20, 21], 0.8)]


def test_match_pending_posts_excludes_already_consumed_cohort_members():
    # 좌 10과 30이 둘 다 시드가 될 수 있고, 둘의 코호트가 같은 글(11)을
    # 놓고 겹침 — 먼저 소비된 쪽이 가져가고, 이후 시드는 그 글을 다시
    # 코호트에 넣으면 안 된다.
    repo = FakeMatchingRepository(
        unmatched_left=[10, 30],
        best_matches={10: (20, 0.8), 30: (40, 0.7)},
        same_side_similar={
            10: [(11, 0.7)],
            30: [(11, 0.65)],
            20: [(21, 0.6)],
            40: [(41, 0.6)],
        },
    )

    matched = match_pending_posts(repo, threshold=0.6, min_posts_per_side=2)

    # 10번 시드가 먼저 11을 코호트로 확보 → 30번 시드는 11이 이미 소비돼
    # 좌측 코호트가 자기 자신뿐이라 min_posts_per_side=2 미달로 생성 안 됨.
    assert matched == 1
    assert repo.created_pairs == [([10, 11], [20, 21], 0.8)]


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

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3, limit=100)

    assert deleted == 1
    assert repo.deleted_posts == [1]


def test_prune_stale_candidates_keeps_post_at_min_cluster_size():
    # 자기 제외 2개 + 자기 자신 1개 = 자기 포함 3개, min_cluster_size=3
    # 이상이라 삭제 안 됨.
    repo = FakeMatchingRepository(prunable_posts=[1], similar_counts={1: 2})

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3, limit=100)

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

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3, limit=100)

    assert deleted == 1
    assert repo.deleted_posts == [2]


def test_prune_stale_candidates_respects_limit():
    # 매칭 안 되는 글은 유예기간 동안 계속 정리 후보로 남을 수 있고,
    # 후보 하나당 count_similar_posts가 전체 임베딩 테이블을 스캔하는
    # 호출을 만든다 — limit을 넘는 후보는 이번 사이클에서 건드리지 않아야
    # 한다(dead_link_scan_limit과 동일한 문제 유형).
    repo = FakeMatchingRepository(
        prunable_posts=[1, 2, 3],
        similar_counts={1: 0, 2: 0, 3: 0},
    )

    deleted = prune_stale_candidates(repo, grace_period_hours=48, min_cluster_size=3, limit=2)

    assert deleted == 2
    assert repo.deleted_posts == [1, 2]


def test_delete_stale_posts_deletes_all_candidates():
    # prune_stale_candidates(벡터 유사도로 "매칭 가능성" 판단)와 달리
    # 단순 보관 기간 정책이라 벡터 계산 없이 후보를 그대로 지운다.
    repo = FakeMatchingRepository(stale_post_ids=[1, 2, 3])

    deleted = delete_stale_posts(repo, retention_days=10, limit=100)

    assert deleted == 3
    assert repo.deleted_posts == [1, 2, 3]


def test_delete_stale_posts_skips_post_that_became_undeletable():
    repo = FakeMatchingRepository(stale_post_ids=[1, 2], undeletable_posts={1})

    deleted = delete_stale_posts(repo, retention_days=10, limit=100)

    assert deleted == 1
    assert repo.deleted_posts == [2]


def test_delete_stale_posts_respects_limit():
    repo = FakeMatchingRepository(stale_post_ids=[1, 2, 3])

    deleted = delete_stale_posts(repo, retention_days=10, limit=2)

    assert deleted == 2
    assert repo.deleted_posts == [1, 2]
