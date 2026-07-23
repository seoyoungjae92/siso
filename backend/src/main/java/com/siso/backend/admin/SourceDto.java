package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record SourceDto(
        Long id,
        String name,
        String side,
        String baseUrl,
        String feedUrl,
        String crawlType,
        boolean enabled,
        OffsetDateTime createdAt) {
}
