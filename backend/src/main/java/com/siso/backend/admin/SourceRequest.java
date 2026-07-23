package com.siso.backend.admin;

public record SourceRequest(String name, String side, String baseUrl, String feedUrl, String crawlType) {
}
