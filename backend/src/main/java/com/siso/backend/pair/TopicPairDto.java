package com.siso.backend.pair;

import java.time.OffsetDateTime;
import java.util.List;

public record TopicPairDto(
        Long id,
        String title,
        String leftStance,
        String rightStance,
        String background,
        List<String> leftPoints,
        List<String> rightPoints,
        List<String> discussionQuestions,
        OffsetDateTime createdAt,
        double leftVotes,
        double rightVotes,
        double neutralVotes,
        long voteCount,
        long commentCount,
        String myStance) {

    public static TopicPairDto from(
            TopicPair pair,
            double leftVotes,
            double rightVotes,
            double neutralVotes,
            long voteCount,
            long commentCount,
            String myStance) {
        return new TopicPairDto(
                pair.getId(),
                pair.getTitle(),
                pair.getLeftStance(),
                pair.getRightStance(),
                pair.getBackground(),
                toList(pair.getLeftPoints()),
                toList(pair.getRightPoints()),
                toList(pair.getDiscussionQuestions()),
                pair.getCreatedAt(),
                leftVotes,
                rightVotes,
                neutralVotes,
                voteCount,
                commentCount,
                myStance);
    }

    // 보강 없는 주제(NULL 배열)도 프론트가 null 체크 없이 쓰도록 빈 목록으로.
    private static List<String> toList(String[] values) {
        return values == null ? List.of() : List.of(values);
    }
}
