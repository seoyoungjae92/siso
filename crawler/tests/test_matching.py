from siso_crawler.matching import embed_pending_posts, match_pending_posts

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
