package com.siso.backend.admin;

import java.time.OffsetDateTime;
import java.util.Map;

public record AdminAlertDto(
        Long id, String type, Map<String, Object> payload, boolean resolved, OffsetDateTime createdAt) {
}
