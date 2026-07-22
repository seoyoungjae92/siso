from siso_crawler.dedupe import hash_url


def test_hash_url_is_deterministic():
    assert hash_url("https://example.test/a") == hash_url("https://example.test/a")


def test_hash_url_differs_for_different_urls():
    assert hash_url("https://example.test/a") != hash_url("https://example.test/b")


def test_hash_url_is_sha256_hex():
    result = hash_url("https://example.test/a")

    assert len(result) == 64
    assert all(c in "0123456789abcdef" for c in result)
