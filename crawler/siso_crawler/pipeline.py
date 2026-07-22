from dataclasses import dataclass

from .dedupe import hash_url
from .models import Source
from .parser import parse_feed
from .repository import PostRepository
from .summarize import summarize


@dataclass
class IngestResult:
    fetched: int = 0
    inserted: int = 0
    skipped_duplicate: int = 0


def ingest_source(source: Source, raw_bytes: bytes, repo: PostRepository) -> IngestResult:
    result = IngestResult()
    for entry in parse_feed(raw_bytes):
        result.fetched += 1
        if not entry.link:
            continue

        url_hash = hash_url(entry.link)
        if repo.exists_by_hash(url_hash):
            result.skipped_duplicate += 1
            continue

        repo.insert_post(
            source_id=source.id,
            title=entry.title,
            summary=summarize(entry.summary),
            origin_url=entry.link,
            origin_url_hash=url_hash,
            published_at=entry.published_at,
        )
        result.inserted += 1

    return result
