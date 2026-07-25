package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record PetitionSettingsDto(String eraco, int topN, int windowDays, OffsetDateTime updatedAt) {
}
