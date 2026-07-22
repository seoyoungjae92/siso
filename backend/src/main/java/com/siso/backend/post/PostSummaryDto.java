package com.siso.backend.post;

import java.time.OffsetDateTime;

public record PostSummaryDto(
        Long id,
        String title,
        String summary,
        String sourceName,
        String originUrl,
        OffsetDateTime publishedAt,
        OffsetDateTime collectedAt) {

    public static PostSummaryDto from(Post post) {
        return new PostSummaryDto(
                post.getId(),
                post.getTitle(),
                post.getSummary(),
                post.getSource().getName(),
                post.getOriginUrl(),
                post.getPublishedAt(),
                post.getCollectedAt());
    }
}
