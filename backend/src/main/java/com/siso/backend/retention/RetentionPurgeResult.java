package com.siso.backend.retention;

import java.time.OffsetDateTime;

public record RetentionPurgeResult(
        int commentsPurged, int anonUsersPurged, int feedbackContactsPurged, OffsetDateTime purgedAt) {
}
