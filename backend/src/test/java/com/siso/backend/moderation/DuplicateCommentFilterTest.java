package com.siso.backend.moderation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateCommentFilterTest {

    private final DuplicateCommentFilter filter = new DuplicateCommentFilter();

    @Test
    void identicalText_isDuplicate() {
        boolean result = filter.isDuplicate("이건 똑같은 댓글입니다", List.of("이건 똑같은 댓글입니다"), 0.85);

        assertThat(result).isTrue();
    }

    @Test
    void nearIdenticalWithTypo_isDuplicate() {
        boolean result = filter.isDuplicate(
                "오늘 날씨가 정말 좋네요 다들 좋은 하루 보내세요",
                List.of("오늘 날씨가 정말 좋네요 다들 좋은 하루 보내셈"),
                0.85);

        assertThat(result).isTrue();
    }

    @Test
    void unrelatedText_isNotDuplicate() {
        boolean result = filter.isDuplicate(
                "전기요금 정책에 대한 제 생각을 말씀드립니다",
                List.of("오늘 점심 메뉴 추천 좀 해주세요"),
                0.85);

        assertThat(result).isFalse();
    }

    @Test
    void emptyRecentList_isNotDuplicate() {
        boolean result = filter.isDuplicate("아무 내용", List.of(), 0.85);

        assertThat(result).isFalse();
    }
}
