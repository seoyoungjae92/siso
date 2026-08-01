package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record CrawlSettingsDto(
        float matchSimilarityThreshold,
        float pruneSimilarityThreshold,
        int minClusterSize,
        int gracePeriodHours,
        int displayWindowDays,
        int synthesisLimit,
        String synthesisModel,
        int deadLinkScanLimit,
        int pruneScanLimit,
        int sourceFailureThreshold,
        OffsetDateTime updatedAt) {
}
