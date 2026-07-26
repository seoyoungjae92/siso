package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record ElectionSettingsDto(boolean enabled, int overrideAutoBlindThreshold, OffsetDateTime updatedAt) {
}
