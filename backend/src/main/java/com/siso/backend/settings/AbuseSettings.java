package com.siso.backend.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "abuse_settings")
public class AbuseSettings {

    @Id
    private Short id;

    @Column(name = "multi_account_cluster_size", nullable = false)
    private int multiAccountClusterSize;

    @Column(name = "multi_account_trust_penalty_multiplier", nullable = false)
    private float multiAccountTrustPenaltyMultiplier;

    @Column(name = "trust_maturity_hours", nullable = false)
    private int trustMaturityHours;

    @Column(name = "trust_min_weight", nullable = false)
    private float trustMinWeight;

    @Column(name = "duplicate_similarity_threshold", nullable = false)
    private float duplicateSimilarityThreshold;

    @Column(name = "duplicate_lookback_count", nullable = false)
    private int duplicateLookbackCount;

    @Column(name = "duplicate_lookback_minutes", nullable = false)
    private int duplicateLookbackMinutes;

    @Column(name = "spike_window_minutes", nullable = false)
    private int spikeWindowMinutes;

    @Column(name = "spike_vote_threshold", nullable = false)
    private int spikeVoteThreshold;

    @Column(name = "spike_reaction_threshold", nullable = false)
    private int spikeReactionThreshold;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AbuseSettings() {
    }

    public Short getId() {
        return id;
    }

    public int getMultiAccountClusterSize() {
        return multiAccountClusterSize;
    }

    public float getMultiAccountTrustPenaltyMultiplier() {
        return multiAccountTrustPenaltyMultiplier;
    }

    public int getTrustMaturityHours() {
        return trustMaturityHours;
    }

    public float getTrustMinWeight() {
        return trustMinWeight;
    }

    public float getDuplicateSimilarityThreshold() {
        return duplicateSimilarityThreshold;
    }

    public int getDuplicateLookbackCount() {
        return duplicateLookbackCount;
    }

    public int getDuplicateLookbackMinutes() {
        return duplicateLookbackMinutes;
    }

    public int getSpikeWindowMinutes() {
        return spikeWindowMinutes;
    }

    public int getSpikeVoteThreshold() {
        return spikeVoteThreshold;
    }

    public int getSpikeReactionThreshold() {
        return spikeReactionThreshold;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            int multiAccountClusterSize,
            float multiAccountTrustPenaltyMultiplier,
            int trustMaturityHours,
            float trustMinWeight,
            float duplicateSimilarityThreshold,
            int duplicateLookbackCount,
            int duplicateLookbackMinutes,
            int spikeWindowMinutes,
            int spikeVoteThreshold,
            int spikeReactionThreshold,
            OffsetDateTime updatedAt) {
        this.multiAccountClusterSize = multiAccountClusterSize;
        this.multiAccountTrustPenaltyMultiplier = multiAccountTrustPenaltyMultiplier;
        this.trustMaturityHours = trustMaturityHours;
        this.trustMinWeight = trustMinWeight;
        this.duplicateSimilarityThreshold = duplicateSimilarityThreshold;
        this.duplicateLookbackCount = duplicateLookbackCount;
        this.duplicateLookbackMinutes = duplicateLookbackMinutes;
        this.spikeWindowMinutes = spikeWindowMinutes;
        this.spikeVoteThreshold = spikeVoteThreshold;
        this.spikeReactionThreshold = spikeReactionThreshold;
        this.updatedAt = updatedAt;
    }
}
