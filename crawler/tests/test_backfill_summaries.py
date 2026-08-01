from unittest.mock import MagicMock

from siso_crawler.backfill_summaries import backfill

from .fakes import FakePostSummarizer


def _conn_returning_rows(rows):
    cursor = MagicMock()
    cursor.fetchall.return_value = rows
    cursor.__enter__.return_value = cursor
    cursor.__exit__.return_value = False
    conn = MagicMock()
    conn.cursor.return_value = cursor
    return conn, cursor


def test_backfill_rewrites_title_and_summary_from_title_alone():
    conn, cursor = _conn_returning_rows([(1, "원본 제목")])
    summarizer = FakePostSummarizer()

    backfill(conn, summarizer)

    assert summarizer.calls == [("원본 제목", "")]
    update_call = cursor.execute.call_args_list[-1]
    assert "UPDATE posts" in update_call.args[0]
    assert update_call.args[1] == ("재작성: 원본 제목", "재작성: ", 1)
    conn.commit.assert_called_once()


def test_backfill_skips_row_on_summarization_failure_without_updating():
    conn, cursor = _conn_returning_rows([(1, "실패할 제목")])
    summarizer = FakePostSummarizer(fail_on={""})

    backfill(conn, summarizer)

    assert cursor.execute.call_args_list == [cursor.execute.call_args_list[0]]  # SELECT만 호출됨
    conn.commit.assert_not_called()
