package com.siso.backend.anon;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class AnonIdHeader {

    private AnonIdHeader() {
    }

    public static UUID parse(String anonId, boolean required) {
        if (anonId == null || anonId.isBlank()) {
            if (required) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id header is required");
            }
            return null;
        }
        try {
            return UUID.fromString(anonId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Anon-Id must be a UUID");
        }
    }
}
