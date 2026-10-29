from datetime import timedelta
from unittest.mock import MagicMock

import psycopg

from siso_crawler import run_lock


def _conn(fetchone):
    cursor = MagicMock()
    cursor.fetchone.return_value = fetchone
    cursor.__enter__.return_value = cursor
    cursor.__exit__.return_value = False
    conn = MagicMock()
    conn.cursor.return_value = cursor
    return conn, cursor


def test_acquire_succeeds_when_row_returned():
    conn, cursor = _conn(("ingest:left",))
    assert run_lock.try_acquire(conn, "ingest:left") is True
    sql, params = cursor.execute.call_args.args
    assert "locked_until < now()" in sql  # 만료된 잠금만 가져온다
    assert params == ("ingest:left", timedelta(hours=2))
    conn.commit.assert_called_once()


def test_acquire_fails_while_previous_run_holds_lease():
    conn, _ = _conn(None)
    assert run_lock.try_acquire(conn, "postprocess:all") is False


def test_release_expires_the_lease():
    conn, cursor = _conn(None)
    run_lock.release(conn, "postprocess:all")
    sql, params = cursor.execute.call_args.args
    assert "locked_until = now()" in sql and params == ("postprocess:all",)
    conn.commit.assert_called_once()


def test_missing_table_runs_without_lock():
    # 백엔드보다 크롤러가 먼저 배포돼 V32가 없을 때도 수집은 멈추지 않는다
    conn, cursor = _conn(None)
    cursor.execute.side_effect = psycopg.errors.UndefinedTable("crawler_run_lock")
    assert run_lock.try_acquire(conn, "ingest:left") is True
    conn.rollback.assert_called_once()
    run_lock.release(conn, "ingest:left")
