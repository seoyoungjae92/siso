package com.siso.backend.admin;

import java.time.OffsetDateTime;

public record AdminFeedbackDto(
        Long id, String category, String body, String contact, String status, OffsetDateTime createdAt) {
}
