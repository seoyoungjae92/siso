package com.siso.backend.admin;

public record ElectionSettingsRequest(boolean enabled, int overrideAutoBlindThreshold) {
}
