from dataclasses import dataclass

from .dedupe import hash_url
from .html_parsers import get_html_parser
from .models import Source
from .parser import RawEntry, parse_feed
from .repository import PostRepository
from .summarize import summarize


@dataclass
class IngestResult:
    fetched: int = 0
    inserted: int = 0
    skipped_duplicate: int = 0


def parse_entries(source: Source, raw_bytes: bytes) -> list[RawEntry]:
    if source.crawl_type == "rss":
        return parse_feed(raw_bytes)

    if source.crawl_type == "html":
        parser = get_html_parser(source.feed_url)
        if parser is None:
            raise ValueError(f"등록된 HTML 파서가 없는 소스: {source.name} ({source.feed_url})")
        return parser(raw_bytes)

    raise ValueError(f"알 수 없는 crawl_type: {source.crawl_type}")


def ingest_source(source: Source, raw_bytes: bytes, repo: PostRepository) -> IngestResult:
    result = IngestResult()
    for entry in parse_entries(source, raw_bytes):
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
