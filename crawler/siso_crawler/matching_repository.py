from typing import Protocol

# count_similar_posts는 "min_cluster_size(기본 3) 넘는지"만 알면 되므로,
# 가장 가까운 이웃 이 개수만 HNSW 인덱스로 훑으면 충분하다 — 여유를 넉넉히
# 둬서 어떤 min_cluster_size 값에도 안전하게 정답을 낸다.
_SIMILAR_POSTS_SCAN_CAP = 50


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

    def find_stale_post_ids(self, retention_days: int, limit: int) -> list[int]: ...

    def delete_post(self, post_id: int) -> bool: ...

    def find_link_check_candidates(self, display_window_days: int, limit: int) -> list[tuple[int, str]]: ...

    def find_pairs_missing_synthesis(
        self, limit: int
    ) -> list[tuple[int, list[tuple[str, str]], list[tuple[str, str]]]]: ...

    def update_pair_synthesis(self, pair_id: int, title: str, left_stance: str, right_stance: str) -> None: ...

    def rollback(self) -> None: ...


class PsycopgMatchingRepository:
    def __init__(self, conn):
        from pgvector.psycopg import register_vector

        register_vector(conn)
        self._conn = conn

    def rollback(self) -> None:
        # postprocess 단계(매칭/정리/데드링크/합성)가 커넥션 하나를 공유하는데,
        # Postgres는 트랜잭션 안 쿼리 하나가 에러 나면 명시적으로 롤백하기
        # 전까지 이후 모든 쿼리를 "current transaction is aborted"로 거부한다
        # — 정리(prune)가 타임아웃 나면 그 뒤 데드링크 정리·주제 합성까지
        # 도미노로 실패하던 버그(2026-08-14, 17시간 연속 합성 실패로 발견).
        self._conn.rollback()

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
        +1 해서 사용한다.

        기존엔 posts 테이블 전체를 자기조인해 모든 행의 벡터 거리를
        계산했다(WHERE절 거리 필터는 HNSW 인덱스를 못 씀) — posts가
        커지면서 이 호출이 정리(prune) 후보 하나당 한 번씩 나가 매
        사이클 statement timeout으로 이어졌다(2026-08-14/31 이틀에
        걸쳐 발견). embedding을 먼저 뽑아 파라미터로 바인딩한 뒤
        `ORDER BY ... LIMIT`으로 물어보면 HNSW 인덱스를 타서 가장 가까운
        이웃 몇 개만 훑는다 — min_cluster_size 판정엔 그 정도면 충분."""
        with self._conn.cursor() as cur:
            cur.execute("SELECT embedding FROM posts WHERE id = %s", (post_id,))
            row = cur.fetchone()
            if row is None or row[0] is None:
                return 0
            embedding = row[0]

            cur.execute(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT embedding <=> %s AS distance
                    FROM posts
                    WHERE id != %s AND embedding IS NOT NULL
                    ORDER BY embedding <=> %s
                    LIMIT %s
                ) nearest
                WHERE (1 - distance) >= %s
                """,
                (embedding, post_id, embedding, _SIMILAR_POSTS_SCAN_CAP, threshold),
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
        # "지금도 cross-side 매칭 후보가 될 수 있는 글은 제외" 조건이 예전엔
        # posts 전체를 자기조인해 모든 상대 후보의 벡터 거리를 계산했다
        # (WHERE절 거리 필터라 HNSW 인덱스를 못 씀) — count_similar_posts와
        # 같은 유형의 문제로, limit(기본 100)건 후보 하나하나가 반대편
        # 전체를 스캔해서 이 함수 자체가 매 사이클 statement timeout으로
        # 이어지고 있었다(2026-09, DB 정리 도중 발견). 유예기간·미매칭
        # 조건으로 먼저 후보를 limit건까지 좁힌 뒤(CTE), 그 후보들에
        # 대해서만 LATERAL JOIN으로 "가장 가까운 반대편 글 1건"을 HNSW
        # 인덱스로 찾는다 — 그 1건조차 임계값 미만이면 임계값 이상인 건
        # 하나도 없다는 뜻이라 기존 NOT EXISTS와 결과가 동일하다.
        with self._conn.cursor() as cur:
            cur.execute(
                """
                WITH candidates AS (
                    SELECT p.id, p.embedding, s1.side
                    FROM posts p
                    JOIN sources s1 ON s1.id = p.source_id
                    WHERE p.collected_at < now() - (%s || ' hours')::interval
                      AND p.embedding IS NOT NULL
                      AND p.topic_pair_id IS NULL
                      AND NOT EXISTS (
                          SELECT 1 FROM comments c WHERE c.post_id = p.id
                      )
                    ORDER BY p.collected_at ASC
                    LIMIT %s
                )
                SELECT c.id
                FROM candidates c
                LEFT JOIN LATERAL (
                    SELECT p2.embedding <=> c.embedding AS distance
                    FROM posts p2
                    JOIN sources s2 ON s2.id = p2.source_id AND s2.side != c.side
                    WHERE p2.embedding IS NOT NULL AND p2.topic_pair_id IS NULL
                    ORDER BY p2.embedding <=> c.embedding
                    LIMIT 1
                ) nearest ON true
                WHERE nearest.distance IS NULL OR (1 - nearest.distance) < %s
                """,
                (grace_period_hours, limit, match_similarity_threshold),
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

    def find_stale_post_ids(self, retention_days: int, limit: int) -> list[int]:
        """단순 보관 기간 정책 — display_window_days가 지나면 피드·
        플레이그라운드 양쪽에서 이미 API 응답에도 안 잡히는 글이라
        (PostService.java/PairService.java 둘 다 같은 기준 사용),
        벡터 유사도 계산 없이 나이+미매칭+무댓글만으로 지운다.
        find_prunable_posts(코호트를 못 채워 "영영 매칭 안 될 것 같은"
        글을 벡터 유사도로 미리 판단)와는 별개 정책 — 이쪽은 매칭
        가능성과 무관하게 그냥 오래돼서 어차피 안 보이는 글을 치운다."""
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT p.id
                FROM posts p
                WHERE p.collected_at < now() - (%s || ' days')::interval
                  AND p.topic_pair_id IS NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM comments c WHERE c.post_id = p.id
                  )
                ORDER BY p.collected_at ASC
                LIMIT %s
                """,
                (retention_days, limit),
            )
            return [row[0] for row in cur.fetchall()]

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
