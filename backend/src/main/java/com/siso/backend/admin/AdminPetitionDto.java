package com.siso.backend.admin;

public record AdminPetitionDto(
        String pttId,
        String title,
        long agreeCount,
        String receivedAt,
        String linkUrl,
        String status,
        String outcome,
        String committeeName,
        String committeeReferredAt,
        Float achvRatio,
        String lastSyncedAt,
        String closedAt) {
}
