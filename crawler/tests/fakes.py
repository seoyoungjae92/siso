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
    ):
        self.pending_embeddings = pending_embeddings or []
        self.unmatched_left = unmatched_left or []
        self.best_matches = best_matches or {}
        self.updated_embeddings: dict[int, list[float]] = {}
        self.created_pairs: list[tuple[int, int, float]] = []

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
