from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class CrawlSettings:
    match_similarity_threshold: float
    prune_similarity_threshold: float
    min_cluster_size: int
    grace_period_hours: int
    display_window_days: int


class SettingsRepository(Protocol):
    def get(self) -> CrawlSettings: ...


class PsycopgSettingsRepository:
    def __init__(self, conn):
        self._conn = conn

    def get(self) -> CrawlSettings:
        with self._conn.cursor() as cur:
            cur.execute(
                """
                SELECT match_similarity_threshold, prune_similarity_threshold,
                       min_cluster_size, grace_period_hours, display_window_days
                FROM crawl_settings WHERE id = 1
                """
            )
            row = cur.fetchone()
        return CrawlSettings(*row)
