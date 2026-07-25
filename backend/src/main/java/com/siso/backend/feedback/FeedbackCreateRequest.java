package com.siso.backend.feedback;

public record FeedbackCreateRequest(String category, String body, String contact) {
}
