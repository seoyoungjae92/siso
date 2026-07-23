package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record SourceStatDto(String sourceName, String side, long postCount, OffsetDateTime lastCollectedAt) {
}
