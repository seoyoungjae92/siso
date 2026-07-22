import httpx


def fetch_feed(url: str, timeout: float = 10.0) -> bytes:
    response = httpx.get(url, timeout=timeout, follow_redirects=True)
    response.raise_for_status()
    return response.content
