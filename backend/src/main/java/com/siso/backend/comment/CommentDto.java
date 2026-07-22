package com.siso.backend.comment;

import java.time.OffsetDateTime;

public record CommentDto(
        Long id,
        Long parentId,
        String nickname,
        String body,
        String stance,
        int upCount,
        int downCount,
        boolean selfReply,
        OffsetDateTime createdAt) {
}
