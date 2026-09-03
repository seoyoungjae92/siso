package com.siso.backend.admin;

public record CrawlSettingsRequest(
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
        float cohortSimilarityThreshold,
        int synthesisMinPostsPerSide,
        int detailFetchLimit,
        int postRetentionDays,
        int stalePostScanLimit) {
}
