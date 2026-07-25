package com.siso.backend.petition;

public record PetitionDto(String id, String title, long agreeCount, String receivedAt, String linkUrl) {
}
