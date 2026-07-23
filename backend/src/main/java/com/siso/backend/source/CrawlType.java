package com.siso.backend.source;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum CrawlType {
    RSS("rss"),
    HTML("html");

    private final String value;

    CrawlType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static CrawlType fromValue(String raw) {
        for (CrawlType crawlType : values()) {
            if (crawlType.value.equalsIgnoreCase(raw)) {
                return crawlType;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "crawlType must be 'rss' or 'html'");
    }
}
