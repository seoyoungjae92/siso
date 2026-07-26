package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record ModerationSettingsDto(
        int autoBlindReportThreshold, String classificationModel, OffsetDateTime updatedAt) {
}
