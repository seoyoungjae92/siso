from typing import Protocol


class MatchingRepository(Protocol):
    def find_posts_missing_embedding(self, limit: int) -> list[tuple[int, str, str]]: ...

    def update_embedding(self, post_id: int, embedding: list[float]) -> None: ...

    def find_unmatched_posts(self, side: str) -> list[int]: ...

    def find_best_cross_side_match(self, post_id: int) -> tuple[int, float] | None: ...

    def find_similar_same_side_posts(self, post_id: int, threshold: float, limit: int) -> list[tuple[int, float]]: ...

    def create_pair(self, left_ids: list[int], right_ids: list[int], similarity: float) -> None: ...

    def count_similar_posts(self, post_id: int, threshold: float) -> int: ...

    def find_prunable_posts(
        self, grace_period_hours: int, match_similarity_threshold: float, limit: int
    ) -> list[int]: ...

    def delete_post(self, post_id: int) -> bool: ...

    def find_link_check_candidates(self, display_window_days: int, limit: int) -> list[tuple[int, str]]: ...

    def find_pairs_missing_synthesis(
        self, limit: int
    ) -> list[tuple[int, list[tuple[str, str]], list[tuple[str, str]]]]: ...

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
                  AND p.topic_pair_id IS NULL
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
                JOIN posts p2 ON p2.embedding IS NOT NULL AND p2.topic_pair_id IS NULL
                JOIN sources s2 ON s2.id = p2.source_id AND s2.side != s1.side
                WHERE p1.id = %s
                ORDER BY p1.embedding <=> p2.embedding
                LIMIT 1
                """,
                (post_id,),
            )
            row = cur.fetchone()
            return (row[0], row[1]) if row else None

    def find_similar_same_side_posts(self, post_id: int, threshold: float, limit: int) -> list[tuple[int, float]]:
        """post_id와 같은 side이고 아직 어떤 주제에도 안 묶인 글 중 유사도
        상위 limit개. 코호트(같은 진영 내 "같은 이야기" 후보) 구성용 —
        시드 글에 대해서만 비교하고 코호트 멤버끼리는 비교하지 않는다
        (별 모양 구조라야 시드 주제에서 이탈하지 않음)."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p2.id, 1 - (p1.embedding <=> p2.embedding) AS similarity
                FROM posts p1
                JOIN sources s1 ON s1.id = p1.source_id
                JOIN posts p2 ON p2.id != p1.id AND p2.embedding IS NOT NULL AND p2.topic_pair_id IS NULL
                JOIN sources s2 ON s2.id = p2.source_id AND s2.side = s1.side
                WHERE p1.id = %s
                  AND (1 - (p1.embedding <=> p2.embedding)) >= %s
                ORDER BY p1.embedding <=> p2.embedding
                LIMIT %s
                """,
                (post_id, threshold, limit),
            )
            return [(row[0], row[1]) for row in cur.fetchall()]

    def create_pair(self, left_ids: list[int], right_ids: list[int], similarity: float) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "INSERT INTO topic_pairs (similarity, matched_by) VALUES (%s, 'auto') RETURNING id",
                (similarity,),
            )
            pair_id = cur.fetchone()[0]
            cur.execute(
                "UPDATE posts SET topic_pair_id = %s WHERE id = ANY(%s)",
                (pair_id, left_ids + right_ids),
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

    def find_prunable_posts(
        self, grace_period_hours: int, match_similarity_threshold: float, limit: int
    ) -> list[int]:
        """유예기간이 지났고, embedding이 있고(임베딩 안 된 글은 아직
        평가 자체가 안 된 것이라 제외), 어떤 주제에도 안 묶였고, 댓글도
        없는 후보. 클러스터 크기 판정은 호출부에서 count_similar_posts로
        별도 확인한다.

        지금도 cross-side 매칭 후보가 될 수 있는 글(=find_best_cross_side_match가
        찾아줄 상대가 있는 글)은 제외한다 — N:M 코호트가 아직 min_posts_per_side
        만큼 안 찼을 뿐인 "성장 중인" 글까지 여기서 삭제해버리면 코호트가
        영영 못 자란다.

        매칭 안 되는 글이 쌓이면 이 후보도 같이 늘어나고, 후보 하나당
        count_similar_posts가 전체 임베딩 테이블을 스캔하는 호출을
        하나씩 만들어서(find_link_check_candidates와 동일한 문제 유형)
        limit으로 사이클당 처리량을 상한. 오래된 것부터 우선 처리하면
        나머지는 유예기간 통과분이 늘어나며 자연 회전한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id
                FROM posts p
                JOIN sources s1 ON s1.id = p.source_id
                WHERE p.collected_at < now() - (%s || ' hours')::interval
                  AND p.embedding IS NOT NULL
                  AND p.topic_pair_id IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = p.id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM posts p2
                      JOIN sources s2 ON s2.id = p2.source_id AND s2.side != s1.side
                      WHERE p2.embedding IS NOT NULL
                        AND p2.topic_pair_id IS NULL
                        AND (1 - (p.embedding <=> p2.embedding)) >= %s
                  )
                ORDER BY p.collected_at ASC
                LIMIT %s
                """,
                (grace_period_hours, match_similarity_threshold, limit),
            )
            return [row[0] for row in cur.fetchall()]

    def find_link_check_candidates(self, display_window_days: int, limit: int) -> list[tuple[int, str]]:
        """화면에 노출 중인(노출 기간 이내) 글만 데드링크 확인 대상으로
        삼는다 — 이미 노출 안 되는 오래된 글까지 매 배치마다 원문 사이트에
        요청을 보낼 필요는 없음. 삭제 안전 조건(참조 여부)은
        find_prunable_posts와 동일하게 어떤 주제에도 안 묶인 글만 대상.

        매칭 안 되는 글은 노출 기간 내내 후보로 남기 때문에 후보가 많이
        쌓이면 한 사이클이 도메인당 10초 간격 정책 때문에 무한정 길어질
        수 있음(실측: 로컬에서 659건 쌓여 110분 예상) — limit으로 사이클당
        처리량을 상한. collected_at 오름차순으로 잘라서, 살아있는 글은
        노출 기간이 끝나갈 무렵(가장 오래된 것부터) 우선 재확인되고, 아직
        안 잘린 나머지는 시간이 지나 오래된 후보들이 기간 만료로 빠지면서
        자연스럽게 순번이 돌아옴."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id, p.origin_url
                FROM posts p
                WHERE p.collected_at > now() - (%s || ' days')::interval
                  AND p.topic_pair_id IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = p.id
                  )
                ORDER BY p.collected_at ASC
                LIMIT %s
                """,
                (display_window_days, limit),
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
                  AND topic_pair_id IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = posts.id
                  )
                """,
                (post_id,),
            )
            deleted = cur.rowcount > 0
        self._conn.commit()
        return deleted

    def find_pairs_missing_synthesis(
        self, limit: int
    ) -> list[tuple[int, list[tuple[str, str]], list[tuple[str, str]]]]:
        """아직 AI 합성이 안 된(title이 NULL인) 쌍만 대상. 최근 매칭된
        것부터 처리해 한정된 limit 예산이 오래된(반복 실패했을 수 있는)
        쌍보다 새로 노출될 쌍에 먼저 쓰이게 한다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id FROM topic_pairs
                WHERE status = 'active' AND title IS NULL
                ORDER BY created_at DESC
                LIMIT %s
                """,
                (limit,),
            )
            pair_ids = [row[0] for row in cur.fetchall()]
            if not pair_ids:
                return []

            cur.execute(
                """
                SELECT p.topic_pair_id, s.side, p.title, p.summary
                FROM posts p
                JOIN sources s ON s.id = p.source_id
                WHERE p.topic_pair_id = ANY(%s)
                """,
                (pair_ids,),
            )
            by_pair: dict[int, dict[str, list[tuple[str, str]]]] = {
                pid: {"left": [], "right": []} for pid in pair_ids
            }
            for pair_id, side, title, summary in cur.fetchall():
                by_pair[pair_id][side].append((title, summary))

        return [(pid, by_pair[pid]["left"], by_pair[pid]["right"]) for pid in pair_ids]

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
