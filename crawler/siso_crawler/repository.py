from typing import Protocol


class PostRepository(Protocol):
    def exists_by_hash(self, origin_url_hash: str) -> bool: ...

    def insert_post(
        self,
        source_id: int,
        title: str,
        summary: str,
        origin_url: str,
        origin_url_hash: str,
        published_at: str | None,
    ) -> None: ...


class PsycopgPostRepository:
    def __init__(self, conn):
        self._conn = conn

    def exists_by_hash(self, origin_url_hash: str) -> bool:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT 1 FROM posts WHERE origin_url_hash = %s", (origin_url_hash,)
            )
            return cur.fetchone() is not None

    def insert_post(
        self,
        source_id: int,
        title: str,
        summary: str,
        origin_url: str,
        origin_url_hash: str,
        published_at: str | None,
    ) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO posts (source_id, title, summary, origin_url, origin_url_hash, published_at)
                VALUES (%s, %s, %s, %s, %s, %s)
                """,
                (source_id, title, summary, origin_url, origin_url_hash, published_at),
            )
        self._conn.commit()
