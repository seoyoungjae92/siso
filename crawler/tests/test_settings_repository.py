from unittest.mock import MagicMock

import psycopg
import pytest

from siso_crawler.settings_repository import CrawlSettings, PsycopgSettingsRepository


def _conn_returning_row(row):
    cursor = MagicMock()
    cursor.fetchone.return_value = row
    cursor.__enter__.return_value = cursor
    cursor.__exit__.return_value = False
    conn = MagicMock()
    conn.cursor.return_value = cursor
    return conn


def test_get_returns_crawl_settings_from_row():
    row = (0.6, 0.5, 3, 48, 7, 10, "openrouter/free", 100, 100, 5)
    conn = _conn_returning_row(row)

    settings = PsycopgSettingsRepository(conn).get()

    assert settings == CrawlSettings(*row)


def test_get_raises_actionable_error_when_migration_not_applied():
    # crawl_settings 스키마는 백엔드(Flyway) 소유라, 새 환경에서 백엔드를
    # 한 번도 안 띄운 채 크롤러부터 돌리면 컬럼이 없어서 실패한다 —
    # 원인을 바로 알 수 있는 메시지로 바꿔야 한다.
    cursor = MagicMock()
    cursor.execute.side_effect = psycopg.errors.UndefinedColumn("column x does not exist")
    cursor.__enter__.return_value = cursor
    cursor.__exit__.return_value = False
    conn = MagicMock()
    conn.cursor.return_value = cursor

    with pytest.raises(RuntimeError, match="백엔드"):
        PsycopgSettingsRepository(conn).get()
