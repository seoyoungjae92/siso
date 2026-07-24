package com.siso.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "moderation_settings")
public class ModerationSettings {

    @Id
    private Short id;

    @Column(name = "auto_blind_report_threshold", nullable = false)
    private int autoBlindReportThreshold;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ModerationSettings() {
    }

    public Short getId() {
        return id;
    }

    public int getAutoBlindReportThreshold() {
        return autoBlindReportThreshold;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(int autoBlindReportThreshold, OffsetDateTime updatedAt) {
        this.autoBlindReportThreshold = autoBlindReportThreshold;
        this.updatedAt = updatedAt;
    }
}
