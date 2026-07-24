from typing import Protocol


class MatchingRepository(Protocol):
    def find_posts_missing_embedding(self, limit: int) -> list[tuple[int, str, str]]: ...

    def update_embedding(self, post_id: int, embedding: list[float]) -> None: ...

    def find_unmatched_posts(self, side: str) -> list[int]: ...

    def find_best_cross_side_match(self, post_id: int) -> tuple[int, float] | None: ...

    def create_pair(self, left_id: int, right_id: int, similarity: float) -> None: ...

    def count_similar_posts(self, post_id: int, threshold: float) -> int: ...

    def find_prunable_posts(self, grace_period_hours: int) -> list[int]: ...

    def delete_post(self, post_id: int) -> bool: ...

    def find_link_check_candidates(self, display_window_days: int) -> list[tuple[int, str]]: ...

    def find_pairs_missing_synthesis(self, limit: int) -> list[tuple[int, str, str, str, str]]: ...

    def update_pair_synthesis(self, pair_id: int, title: str, left_stance: str, right_stance: str) -> None: ...


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

    def count_similar_posts(self, post_id: int, threshold: float) -> int:
        """post_id 자신을 제외하고, 좌/우 구분 없이 유사도 임계값 이상인
        다른 글의 개수. 클러스터 크기 판정("자기 포함 N개") 시 호출부에서
        +1 해서 사용한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT COUNT(*)
                FROM posts p1
                JOIN posts p2 ON p2.id != p1.id AND p2.embedding IS NOT NULL
                WHERE p1.id = %s
                  AND (1 - (p1.embedding <=> p2.embedding)) >= %s
                """,
                (post_id, threshold),
            )
            return cur.fetchone()[0]

    def find_prunable_posts(self, grace_period_hours: int) -> list[int]:
        """유예기간이 지났고, embedding이 있고(임베딩 안 된 글은 아직
        평가 자체가 안 된 것이라 제외), topic_pairs/comments에 상태와
        무관하게 참조되지 않은 후보. 클러스터 크기 판정은 호출부에서
        count_similar_posts로 별도 확인한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id
                FROM posts p
                WHERE p.collected_at < now() - (%s || ' hours')::interval
                  AND p.embedding IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM topic_pairs tp
                      WHERE tp.left_post_id = p.id OR tp.right_post_id = p.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = p.id
                  )
                """,
                (grace_period_hours,),
            )
            return [row[0] for row in cur.fetchall()]

    def find_link_check_candidates(self, display_window_days: int) -> list[tuple[int, str]]:
        """화면에 노출 중인(노출 기간 이내) 글만 데드링크 확인 대상으로
        삼는다 — 이미 노출 안 되는 오래된 글까지 매 배치마다 원문 사이트에
        요청을 보낼 필요는 없음. 삭제 안전 조건(참조 여부)은
        find_prunable_posts와 동일하게 상태 무관 전체 참조를 배제한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id, p.origin_url
                FROM posts p
                WHERE p.collected_at > now() - (%s || ' days')::interval
                  AND NOT EXISTS (
                      SELECT 1 FROM topic_pairs tp
                      WHERE tp.left_post_id = p.id OR tp.right_post_id = p.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = p.id
                  )
                """,
                (display_window_days,),
            )
            return [(row[0], row[1]) for row in cur.fetchall()]

    def delete_post(self, post_id: int) -> bool:
        """조회~삭제 사이에 댓글/매칭이 새로 생겼을 수 있으므로 DELETE
        자체의 WHERE 절에도 참조 조건을 다시 확인한다 — 조건에 안 맞으면
        조용히 0행 삭제로 끝나고 FK 위반 에러가 나지 않는다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                DELETE FROM posts
                WHERE id = %s
                  AND NOT EXISTS (
                      SELECT 1 FROM topic_pairs tp
                      WHERE tp.left_post_id = posts.id OR tp.right_post_id = posts.id
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = posts.id
                  )
                """,
                (post_id,),
            )
            deleted = cur.rowcount > 0
        self._conn.commit()
        return deleted

    def find_pairs_missing_synthesis(self, limit: int) -> list[tuple[int, str, str, str, str]]:
        """아직 AI 합성이 안 된(title이 NULL인) 쌍만 대상. 최근 매칭된
        것부터 처리해 한정된 limit 예산이 오래된(반복 실패했을 수 있는)
        쌍보다 새로 노출될 쌍에 먼저 쓰이게 한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT tp.id, p1.title, p1.summary, p2.title, p2.summary
                FROM topic_pairs tp
                JOIN posts p1 ON p1.id = tp.left_post_id
                JOIN posts p2 ON p2.id = tp.right_post_id
                WHERE tp.status = 'active' AND tp.title IS NULL
                ORDER BY tp.created_at DESC
                LIMIT %s
                """,
                (limit,),
            )
            return [(row[0], row[1], row[2], row[3], row[4]) for row in cur.fetchall()]

    def update_pair_synthesis(self, pair_id: int, title: str, left_stance: str, right_stance: str) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                UPDATE topic_pairs
                SET title = %s, left_stance = %s, right_stance = %s
                WHERE id = %s
                """,
                (title, left_stance, right_stance, pair_id),
            )
        self._conn.commit()
