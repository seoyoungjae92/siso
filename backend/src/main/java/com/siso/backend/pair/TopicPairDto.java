package com.siso.backend.pair;

import com.siso.backend.post.PostSummaryDto;

import java.time.OffsetDateTime;

public record TopicPairDto(
        Long id,
        float similarity,
        OffsetDateTime createdAt,
        PostSummaryDto leftPost,
        PostSummaryDto rightPost) {

    public static TopicPairDto from(TopicPair pair) {
        return new TopicPairDto(
                pair.getId(),
                pair.getSimilarity(),
                pair.getCreatedAt(),
                PostSummaryDto.from(pair.getLeftPost()),
                PostSummaryDto.from(pair.getRightPost()));
    }
}
