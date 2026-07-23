from typing import Protocol

from .models import Source


class SourceRepository(Protocol):
    def find_enabled(self) -> list[Source]: ...


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
