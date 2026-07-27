from siso_crawler.profanity import contains_banned_word


def test_contains_banned_word_detects_known_slur():
    assert contains_banned_word("창녀") is True


def test_contains_banned_word_detects_word_within_longer_text():
    assert contains_banned_word("이 사람 진짜 병신이네") is True


def test_contains_banned_word_ignores_whitespace_between_characters():
    assert contains_banned_word("창 녀") is True


def test_contains_banned_word_false_for_clean_text():
    assert contains_banned_word("오늘 날씨가 좋네요") is False
