package com.siso.backend.feedback;

import jakarta.validation.constraints.Size;

public record FeedbackCreateRequest(
        String category, @Size(max = 2000) String body, @Size(max = 200) String contact) {
}
