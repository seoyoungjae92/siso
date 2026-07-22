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
