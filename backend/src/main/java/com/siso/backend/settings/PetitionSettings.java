package com.siso.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "petition_settings")
public class PetitionSettings {

    @Id
    private Short id;

    @Column(name = "eraco", nullable = false)
    private String eraco;

    @Column(name = "top_n", nullable = false)
    private int topN;

    @Column(name = "window_days", nullable = false)
    private int windowDays;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected PetitionSettings() {
    }

    public Short getId() {
        return id;
    }

    public String getEraco() {
        return eraco;
    }

    public int getTopN() {
        return topN;
    }

    public int getWindowDays() {
        return windowDays;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String eraco, int topN, int windowDays, OffsetDateTime updatedAt) {
        this.eraco = eraco;
        this.topN = topN;
        this.windowDays = windowDays;
        this.updatedAt = updatedAt;
    }
}
