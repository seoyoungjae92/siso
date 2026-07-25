package com.siso.backend.pair;

import java.time.OffsetDateTime;

public record TopicPairDto(
        Long id,
        String title,
        String leftStance,
        String rightStance,
        OffsetDateTime createdAt,
        double leftVotes,
        double rightVotes,
        double neutralVotes,
        long commentCount,
        String myStance) {

    public static TopicPairDto from(
            TopicPair pair,
            double leftVotes,
            double rightVotes,
            double neutralVotes,
            long commentCount,
            String myStance) {
        return new TopicPairDto(
                pair.getId(),
                pair.getTitle(),
                pair.getLeftStance(),
                pair.getRightStance(),
                pair.getCreatedAt(),
                leftVotes,
                rightVotes,
                neutralVotes,
                commentCount,
                myStance);
    }
}
