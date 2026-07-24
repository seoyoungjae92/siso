package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record AbuseSettingsDto(
        int multiAccountClusterSize,
        float multiAccountTrustPenaltyMultiplier,
        int trustMaturityHours,
        float trustMinWeight,
        float duplicateSimilarityThreshold,
        int duplicateLookbackCount,
        int duplicateLookbackMinutes,
        int spikeWindowMinutes,
        int spikeVoteThreshold,
        int spikeReactionThreshold,
        OffsetDateTime updatedAt) {
}
