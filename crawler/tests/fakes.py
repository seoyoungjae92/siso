class FakePostRepository:
    def __init__(self, existing_hashes: set[str] | None = None):
        self.existing_hashes = set(existing_hashes or set())
        self.inserted: list[dict] = []

    def exists_by_hash(self, origin_url_hash: str) -> bool:
        return origin_url_hash in self.existing_hashes

    def insert_post(
        self,
        source_id: int,
        title: str,
        summary: str,
        origin_url: str,
        origin_url_hash: str,
        published_at: str | None,
    ) -> None:
        self.inserted.append(
            {
                "source_id": source_id,
                "title": title,
                "summary": summary,
                "origin_url": origin_url,
                "origin_url_hash": origin_url_hash,
                "published_at": published_at,
            }
        )
        self.existing_hashes.add(origin_url_hash)


class FakeEmbeddingProvider:
    def __init__(self):
        self.embedded_texts: list[str] = []

    def embed(self, text: str) -> list[float]:
        self.embedded_texts.append(text)
        return [float(len(text))]


class FakeMatchingRepository:
    def __init__(
        self,
        pending_embeddings: list[tuple[int, str, str]] | None = None,
        unmatched_left: list[int] | None = None,
        best_matches: dict[int, tuple[int, float]] | None = None,
        similar_counts: dict[int, int] | None = None,
        prunable_posts: list[int] | None = None,
        undeletable_posts: set[int] | None = None,
        link_check_candidates: list[tuple[int, str]] | None = None,
        pairs_missing_synthesis: list[tuple[int, str, str, str, str]] | None = None,
    ):
        self.pending_embeddings = pending_embeddings or []
        self.unmatched_left = unmatched_left or []
        self.best_matches = best_matches or {}
        self.updated_embeddings: dict[int, list[float]] = {}
        self.created_pairs: list[tuple[int, int, float]] = []
        self.similar_counts = similar_counts or {}
        self.prunable_posts = prunable_posts or []
        self.undeletable_posts = undeletable_posts or set()
        self.deleted_posts: list[int] = []
        self.link_check_candidates = link_check_candidates or []
        self.pairs_missing_synthesis = pairs_missing_synthesis or []
        self.synthesized_pairs: list[tuple[int, str, str, str]] = []

    def find_posts_missing_embedding(self, limit: int) -> list[tuple[int, str, str]]:
        return self.pending_embeddings[:limit]

    def update_embedding(self, post_id: int, embedding: list[float]) -> None:
        self.updated_embeddings[post_id] = embedding

    def find_unmatched_posts(self, side: str) -> list[int]:
        return self.unmatched_left if side == "left" else []

    def find_best_cross_side_match(self, post_id: int) -> tuple[int, float] | None:
        return self.best_matches.get(post_id)

    def create_pair(self, left_id: int, right_id: int, similarity: float) -> None:
        self.created_pairs.append((left_id, right_id, similarity))

    def count_similar_posts(self, post_id: int, threshold: float) -> int:
        return self.similar_counts.get(post_id, 0)

    def find_prunable_posts(self, grace_period_hours: int) -> list[int]:
        return self.prunable_posts

    def delete_post(self, post_id: int) -> bool:
        if post_id in self.undeletable_posts:
            return False
        self.deleted_posts.append(post_id)
        return True

    def find_link_check_candidates(self, display_window_days: int) -> list[tuple[int, str]]:
        return self.link_check_candidates

    def find_pairs_missing_synthesis(self, limit: int) -> list[tuple[int, str, str, str, str]]:
        return self.pairs_missing_synthesis[:limit]

    def update_pair_synthesis(self, pair_id: int, title: str, left_stance: str, right_stance: str) -> None:
        self.synthesized_pairs.append((pair_id, title, left_stance, right_stance))


class FakeTopicSynthesizer:
    """(left_title, right_title) 키로 미리 정해둔 결과를 반환. fail_keys에
    있는 키는 SynthesisFailed를 던져서 실패-격리 경로를 테스트한다."""

    def __init__(self, results: dict, fail_keys: set | None = None):
        self.results = results
        self.fail_keys = fail_keys or set()

    def synthesize(self, left_title, left_summary, right_title, right_summary):
        from siso_crawler.llm_client import SynthesisFailed

        key = (left_title, right_title)
        if key in self.fail_keys or key not in self.results:
            raise SynthesisFailed(f"no fixture for {key}")
        return self.results[key]
