import json
from typing import Protocol

from .models import Source

# 200 OK인데 응답이 0바이트인 현상(디시인사이드에서 반복 관측, 2026-08-14/
# 2026-09-04) — 실제로는 사이트의 레이트리밋 스로틀인데, 기존엔 이걸 그냥
# "정상 fetch, 파싱 0건"으로 처리하고 매 사이클 똑같은 간격으로 계속
# 두드려서 스로틀이 풀릴 기회가 없었다(24시간 넘게 전량 0바이트로 확인됨).
# consecutive_failures(네트워크 자체 실패 전용, 5회면 자동 비활성화)와는
# 별개로 카운트해서, 비활성화 없이 지수 백오프만 건다.
THROTTLE_BASE_SECONDS = 30 * 60
THROTTLE_MAX_SECONDS = 6 * 60 * 60


class SourceRepository(Protocol):
    def find_enabled(self) -> list[Source]: ...

    def record_failure(self, source_id: int) -> int: ...

    def record_success(self, source_id: int) -> None: ...

    def disable_and_alert(self, source_id: int, source_name: str, consecutive_failures: int) -> None: ...

    def record_throttle(self, source_id: int) -> float: ...

    def clear_throttle(self, source_id: int) -> None: ...


class PsycopgSourceRepository:
    def __init__(self, conn):
        self._conn = conn

    def find_enabled(self) -> list[Source]:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, name, side, base_url, feed_url, crawl_type, enabled, throttled_until
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

    def record_throttle(self, source_id: int) -> float:
        """스로틀(200+빈 응답) 감지 시 strikes를 늘리고 지수 백오프로
        throttled_until을 설정한다. 반환값은 이번에 적용된 쿨다운(초) —
        호출부 로그용."""
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE sources SET throttle_strikes = throttle_strikes + 1 "
                "WHERE id = %s RETURNING throttle_strikes",
                (source_id,),
            )
            strikes = cur.fetchone()[0]
            cooldown_seconds = min(
                THROTTLE_BASE_SECONDS * (2 ** (strikes - 1)), THROTTLE_MAX_SECONDS
            )
            cur.execute(
                "UPDATE sources SET throttled_until = now() + (%s || ' seconds')::interval "
                "WHERE id = %s",
                (cooldown_seconds, source_id),
            )
        self._conn.commit()
        return cooldown_seconds

    def clear_throttle(self, source_id: int) -> None:
        with self._conn.cursor() as cur:
            cur.execute(
                "UPDATE sources SET throttle_strikes = 0, throttled_until = NULL "
                "WHERE id = %s AND (throttle_strikes != 0 OR throttled_until IS NOT NULL)",
                (source_id,),
            )
        self._conn.commit()
