package com.siso.backend.pair;

import com.siso.backend.post.PostSummaryDto;

import java.time.OffsetDateTime;

public record TopicPairDto(
        Long id,
        float similarity,
        OffsetDateTime createdAt,
        PostSummaryDto leftPost,
        PostSummaryDto rightPost,
        long leftVotes,
        long rightVotes,
        long neutralVotes,
        String myStance) {

    public static TopicPairDto from(TopicPair pair) {
        return from(pair, 0, 0, 0, null);
    }

    public static TopicPairDto from(
            TopicPair pair, long leftVotes, long rightVotes, long neutralVotes, String myStance) {
        return new TopicPairDto(
                pair.getId(),
                pair.getSimilarity(),
                pair.getCreatedAt(),
                PostSummaryDto.from(pair.getLeftPost()),
                PostSummaryDto.from(pair.getRightPost()),
                leftVotes,
                rightVotes,
                neutralVotes,
                myStance);
    }
}
