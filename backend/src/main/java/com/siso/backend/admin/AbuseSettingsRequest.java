package com.siso.backend.admin;

public record AbuseSettingsRequest(
        int multiAccountClusterSize,
        float multiAccountTrustPenaltyMultiplier,
        int trustMaturityHours,
        float trustMinWeight,
        float duplicateSimilarityThreshold,
        int duplicateLookbackCount,
        int duplicateLookbackMinutes,
        int spikeWindowMinutes,
        int spikeVoteThreshold,
        int spikeReactionThreshold) {
}
