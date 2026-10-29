"""같은 크론이 겹쳐 도는 걸 막는 임대 잠금(crawler_run_lock, V32).

이전 실행이 길어져 다음 정각 크론과 겹치면 같은 글을 두 번 매칭하거나
같은 주제를 두 번 합성할 수 있었다. 만료 시각이 지난 잠금만 가져올 수
있어서, 크롤러가 중간에 죽어도 lease가 지나면 다음 실행이 정상으로 돈다.
"""
from __future__ import annotations

import logging
from datetime import timedelta

import psycopg

logger = logging.getLogger(__name__)

LEASE = timedelta(hours=2)


def try_acquire(conn, name: str, lease: timedelta = LEASE) -> bool:
    try:
        return _try_acquire(conn, name, lease)
    except psycopg.errors.UndefinedTable:
        # 백엔드(Flyway)가 V32를 아직 안 돌린 상태로 크롤러가 먼저 뜬 경우.
        # 잠금 때문에 수집 자체가 멈추면 안 되니 예전처럼 잠금 없이 돈다.
        conn.rollback()
        logger.warning("crawler_run_lock 테이블이 없어 잠금 없이 실행합니다(V32 미적용)")
        return True


def _try_acquire(conn, name: str, lease: timedelta) -> bool:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO crawler_run_lock (name, locked_until)
            VALUES (%s, now() + %s)
            ON CONFLICT (name) DO UPDATE SET locked_until = EXCLUDED.locked_until
             WHERE crawler_run_lock.locked_until < now()
            RETURNING name
            """,
            (name, lease),
        )
        acquired = cur.fetchone() is not None
    conn.commit()
    return acquired


def release(conn, name: str) -> None:
    try:
        with conn.cursor() as cur:
            cur.execute("UPDATE crawler_run_lock SET locked_until = now() WHERE name = %s", (name,))
        conn.commit()
    except psycopg.errors.UndefinedTable:
        conn.rollback()
