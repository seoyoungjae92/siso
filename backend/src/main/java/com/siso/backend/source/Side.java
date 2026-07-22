package com.siso.backend.source;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public enum Side {
    LEFT("left"),
    RIGHT("right");

    private final String value;

    Side(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Side fromValue(String raw) {
        for (Side side : values()) {
            if (side.value.equalsIgnoreCase(raw)) {
                return side;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "side must be 'left' or 'right'");
    }
}
