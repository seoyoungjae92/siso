package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record BlindHistoryDto(
        Long alertId,
        String type,
        Long commentId,
        String commentBody,
        String nickname,
        Long pairId,
        Integer reportCount,
        OffsetDateTime createdAt) {
}
