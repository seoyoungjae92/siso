import hashlib


def hash_url(origin_url: str) -> str:
    return hashlib.sha256(origin_url.encode("utf-8")).hexdigest()
