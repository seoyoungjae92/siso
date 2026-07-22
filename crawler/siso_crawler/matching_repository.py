from typing import Protocol


class MatchingRepository(Protocol):
    def find_posts_missing_embedding(self, limit: int) -> list[tuple[int, str, str]]: ...

    def update_embedding(self, post_id: int, embedding: list[float]) -> None: ...

    def find_unmatched_posts(self, side: str) -> list[int]: ...

    def find_best_cross_side_match(self, post_id: int) -> tuple[int, float] | None: ...

    def create_pair(self, left_id: int, right_id: int, similarity: float) -> None: ...


class PsycopgMatchingRepository:
    def __init__(self, conn):
        from pgvector.psycopg import register_vector

        register_vector(conn)
        self._conn = conn

    def find_posts_missing_embedding(self, limit: int) -> list[tuple[int, str, str]]:
        with self._conn.cursor() as cur:
            cur.execute(
                "SELECT id, title, summary FROM posts WHERE embedding IS NULL LIMIT %s",
                (limit,),
            )
            return cur.fetchall()

    def update_embedding(self, post_id: int, embedding: list[float]) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE posts SET embedding = %s WHERE id = %s", (embedding, post_id)
            )
        self._conn.commit()

    def find_unmatched_posts(self, side: str) -> list[int]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id
                FROM posts p
                JOIN sources s ON s.id = p.source_id
                WHERE s.side = %s
                  AND p.embedding IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM topic_pairs tp
                      WHERE tp.status = 'active'
                        AND (tp.left_post_id = p.id OR tp.right_post_id = p.id)
                  )
                """,
                (side,),
            )
            return [row[0] for row in cur.fetchall()]

    def find_best_cross_side_match(self, post_id: int) -> tuple[int, float] | None:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p2.id, 1 - (p1.embedding <=> p2.embedding) AS similarity
                FROM posts p1
                JOIN sources s1 ON s1.id = p1.source_id
                JOIN posts p2 ON p2.embedding IS NOT NULL
                JOIN sources s2 ON s2.id = p2.source_id AND s2.side != s1.side
                WHERE p1.id = %s
                  AND NOT EXISTS (
                      SELECT 1 FROM topic_pairs tp
                      WHERE tp.status = 'active'
                        AND (tp.left_post_id = p2.id OR tp.right_post_id = p2.id)
                  )
                ORDER BY p1.embedding <=> p2.embedding
                LIMIT 1
                """,
                (post_id,),
            )
            row = cur.fetchone()
            return (row[0], row[1]) if row else None

    def create_pair(self, left_id: int, right_id: int, similarity: float) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO topic_pairs (left_post_id, right_post_id, similarity, matched_by)
                VALUES (%s, %s, %s, 'auto')
                """,
                (left_id, right_id, similarity),
            )
        self._conn.commit()
