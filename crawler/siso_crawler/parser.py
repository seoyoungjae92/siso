from __future__ import annotations

from dataclasses import dataclass

import feedparser


@dataclass(frozen=True)
class RawEntry:
    title: str
    link: str
    summary: str
    published_at: str | None


def parse_feed(raw_bytes: bytes) -> list[RawEntry]:
    parsed = feedparser.parse(raw_bytes)
    entries: list[RawEntry] = []
    for entry in parsed.entries:
        entries.append(
            RawEntry(
                title=entry.get("title", "").strip(),
                link=entry.get("link", "").strip(),
                summary=entry.get("summary", "").strip(),
                published_at=entry.get("published"),
            )
        )
    return entries
