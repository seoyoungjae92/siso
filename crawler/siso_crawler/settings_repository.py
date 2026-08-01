from dataclasses import dataclass
from typing import Protocol

import psycopg


@dataclass(frozen=True)
class CrawlSettings:
    match_similarity_threshold: float
    prune_similarity_threshold: float
    min_cluster_size: int
    grace_period_hours: int
    display_window_days: int
    synthesis_limit: int = 10
    synthesis_model: str = "openrouter/free"
    dead_link_scan_limit: int = 100
    prune_scan_limit: int = 100
    source_failure_threshold: int = 5
    cohort_similarity_threshold: float = 0.5
    synthesis_min_posts_per_side: int = 1


class SettingsRepository(Protocol):
    def get(self) -> CrawlSettings: ...


class PsycopgSettingsRepository:
    """crawl_settings 컬럼 목록은 백엔드(Java/Flyway) 소유 마이그레이션이
    정의한다 — 크롤러는 이 테이블을 읽기만 하고 스키마를 직접 관리하지
    않는다. 그래서 새 배포 환경에서 백엔드를 한 번도 기동시키지 않은 채
    크롤러부터 돌리면(마이그레이션 미적용) 이 컬럼들이 아직 없어서
    실패한다 — 원인을 바로 알 수 있게 아래에서 에러 메시지를 명확히
    한다."""

    def __init__(self, conn):
        self._conn = conn

    def get(self) -> CrawlSettings:
        try:
            with self._conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT match_similarity_threshold, prune_similarity_threshold,
                           min_cluster_size, grace_period_hours, display_window_days,
                           synthesis_limit, synthesis_model, dead_link_scan_limit,
                           prune_scan_limit, source_failure_threshold,
                           cohort_similarity_threshold, synthesis_min_posts_per_side
                    FROM crawl_settings WHERE id = 1
                    """
                )
                row = cur.fetchone()
        except psycopg.errors.UndefinedColumn as exc:
            raise RuntimeError(
                "crawl_settings 테이블에 필요한 컬럼이 없습니다 — 백엔드(Spring Boot)를 "
                "한 번도 기동하지 않아 Flyway 마이그레이션이 아직 적용 안 된 DB일 수 "
                "있습니다. 크롤러를 돌리기 전에 백엔드를 먼저 배포/기동해주세요."
            ) from exc
        return CrawlSettings(*row)
