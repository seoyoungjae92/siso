package com.siso.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "crawl_settings")
public class CrawlSettings {

    @Id
    private Short id;

    @Column(name = "match_similarity_threshold", nullable = false)
    private float matchSimilarityThreshold;

    @Column(name = "prune_similarity_threshold", nullable = false)
    private float pruneSimilarityThreshold;

    @Column(name = "min_cluster_size", nullable = false)
    private int minClusterSize;

    @Column(name = "grace_period_hours", nullable = false)
    private int gracePeriodHours;

    @Column(name = "display_window_days", nullable = false)
    private int displayWindowDays;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected CrawlSettings() {
    }

    public Short getId() {
        return id;
    }

    public float getMatchSimilarityThreshold() {
        return matchSimilarityThreshold;
    }

    public float getPruneSimilarityThreshold() {
        return pruneSimilarityThreshold;
    }

    public int getMinClusterSize() {
        return minClusterSize;
    }

    public int getGracePeriodHours() {
        return gracePeriodHours;
    }

    public int getDisplayWindowDays() {
        return displayWindowDays;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            float matchSimilarityThreshold,
            float pruneSimilarityThreshold,
            int minClusterSize,
            int gracePeriodHours,
            int displayWindowDays,
            OffsetDateTime updatedAt) {
        this.matchSimilarityThreshold = matchSimilarityThreshold;
        this.pruneSimilarityThreshold = pruneSimilarityThreshold;
        this.minClusterSize = minClusterSize;
        this.gracePeriodHours = gracePeriodHours;
        this.displayWindowDays = displayWindowDays;
        this.updatedAt = updatedAt;
    }
}
