package com.siso.backend.admin;

public record CrawlSettingsRequest(
        float matchSimilarityThreshold,
        float pruneSimilarityThreshold,
        int minClusterSize,
        int gracePeriodHours,
        int displayWindowDays) {
}
