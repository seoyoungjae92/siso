package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record ModerationSettingsDto(int autoBlindReportThreshold, OffsetDateTime updatedAt) {
}
