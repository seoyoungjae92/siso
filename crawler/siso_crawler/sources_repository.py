import json
from typing import Protocol

from .models import Source


class SourceRepository(Protocol):
    def find_enabled(self) -> list[Source]: ...

    def record_failure(self, source_id: int) -> int: ...

    def record_success(self, source_id: int) -> None: ...

    def disable_and_alert(self, source_id: int, source_name: str, consecutive_failures: int) -> None: ...


class PsycopgSourceRepository:
    def __init__(self, conn):
        self._conn = conn

    def find_enabled(self) -> list[Source]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, name, side, base_url, feed_url, crawl_type, enabled
                FROM sources WHERE enabled = true ORDER BY id
                """
            )
            rows = cur.fetchall()
        return [Source(*row) for row in rows]

    def record_failure(self, source_id: int) -> int:
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE sources SET consecutive_failures = consecutive_failures + 1 "
                "WHERE id = %s RETURNING consecutive_failures",
                (source_id,),
            )
            count = cur.fetchone()[0]
        self._conn.commit()
        return count

    def record_success(self, source_id: int) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE sources SET consecutive_failures = 0 WHERE id = %s AND consecutive_failures != 0",
                (source_id,),
            )
        self._conn.commit()

    def disable_and_alert(self, source_id: int, source_name: str, consecutive_failures: int) -> None:
        """CLAUDE.md §4.2: 실패/차단 감지 시 해당 소스 자동 비활성화 +
        관리자 알림. 관리자는 /admin/sources에서 비활성화된 소스와 연속
        실패 횟수를 바로 볼 수 있고, admin_alerts에도 남겨서 감사 이력을
        보존한다(백엔드의 다른 알림 타입들과 동일한 테이블 재사용)."""
        with self._conn.cursor() as cur:
            cur.execute("UPDATE sources SET enabled = false WHERE id = %s", (source_id,))
            payload = json.dumps(
                {"sourceId": source_id, "sourceName": source_name, "consecutiveFailures": consecutive_failures}
            )
            cur.execute(
                "INSERT INTO admin_alerts (type, payload, created_at) VALUES (%s, %s::jsonb, now())",
                ("source_disabled", payload),
            )
        self._conn.commit()
