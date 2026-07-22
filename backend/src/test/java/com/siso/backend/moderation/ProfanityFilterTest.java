package com.siso.backend.moderation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    void containsBannedWord_detectsKnownWord() {
        assertThat(filter.containsBannedWord("이건 진짜 병신같은 정책이다")).isTrue();
    }

    @Test
    void containsBannedWord_detectsWordWithInnerSpacing() {
        assertThat(filter.containsBannedWord("시 발 진짜")).isTrue();
    }

    @Test
    void containsBannedWord_allowsCleanText() {
        assertThat(filter.containsBannedWord("이 정책에는 동의하기 어렵습니다")).isFalse();
    }
}
