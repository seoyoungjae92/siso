from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class Source:
    id: int
    name: str
    side: str
    base_url: str
    feed_url: str | None
    crawl_type: str
    enabled: bool
    throttled_until: datetime | None = None
