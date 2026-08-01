from __future__ import annotations

import logging
import os
import time

import psycopg

from .llm_client import build_post_summarizer
from .settings_repository import PsycopgSettingsRepository
from .summarize import summarize

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# OPENROUTER_API_KEY 무료 티어 일일 한도(50건) 소진으로 요약이 실패했던
# 기간에 summary=''로 저장된 기존 글들을 재요약한다(1회성). 크롤러는
# origin_url_hash 중복 체크 때문에 재수집으로는 이 글들을 다시 안 건드리므로
# (pipeline.py의 exists_by_hash), 별도 UPDATE 스크립트가 필요함. HTML 소스는
# 원문 본문을 애초에 저장하지 않아(html_parsers.py) 제목만으로 재작성한다.
REQUEST_INTERVAL_SECONDS = 0.5


def backfill(conn, summarizer) -> None:
    with conn.cursor() as cur:
        cur.execute("SELECT id, title FROM posts WHERE summary = ''")
        rows = cur.fetchall()

    logger.info("백필 대상: %d건", len(rows))
    updated = 0
    failed = 0
    for post_id, title in rows:
        result = summarize("", title=title, summarizer=summarizer)
        if not result.summary:
            logger.warning("빈 요약 반환, 건너뜀(postId=%d)", post_id)
            failed += 1
            continue

        with conn.cursor() as cur:
            cur.execute(
                "UPDATE posts SET title = %s, summary = %s WHERE id = %s",
                (result.title, result.summary, post_id),
            )
        conn.commit()
        updated += 1
        time.sleep(REQUEST_INTERVAL_SECONDS)

    logger.info("백필 완료: updated=%d failed=%d", updated, failed)


def main() -> None:
    database_url = os.environ["CRAWLER_DATABASE_URL"]
    with psycopg.connect(database_url) as conn:
        settings = PsycopgSettingsRepository(conn).get()
        api_key = os.environ.get("OPENROUTER_API_KEY")
        summarizer = build_post_summarizer(api_key, model=settings.synthesis_model)
        if summarizer is None:
            raise RuntimeError("OPENROUTER_API_KEY 미설정 — 백필 불가")
        backfill(conn, summarizer)


if __name__ == "__main__":
    main()
