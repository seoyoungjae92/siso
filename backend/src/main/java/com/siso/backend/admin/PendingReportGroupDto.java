package com.siso.backend.admin;

import java.time.OffsetDateTime;
import java.util.Map;

public record PendingReportGroupDto(
        Long commentId,
        String commentBody,
        String nickname,
        Long pairId,
        Map<String, Long> reasonCounts,
        long totalReports,
        OffsetDateTime oldestReportAt) {
}
