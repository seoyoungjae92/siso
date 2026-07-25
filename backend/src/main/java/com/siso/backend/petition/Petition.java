package com.siso.backend.petition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "petitions")
public class Petition {

    public static final String STATUS_COLLECTING = "collecting";
    public static final String STATUS_CLOSED = "closed";
    public static final String OUTCOME_ESTABLISHED = "established";
    public static final String OUTCOME_NOT_ESTABLISHED = "not_established";

    @Id
    @Column(name = "ptt_id")
    private String pttId;

    @Column(name = "eraco", nullable = false)
    private String eraco;

    @Column(name = "ptt_no", nullable = false)
    private String pttNo;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "agree_count", nullable = false)
    private long agreeCount;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Column(name = "link_url", nullable = false)
    private String linkUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "outcome")
    private String outcome;

    @Column(name = "committee_name")
    private String committeeName;

    @Column(name = "committee_referred_at")
    private LocalDate committeeReferredAt;

    @Column(name = "achv_ratio")
    private Float achvRatio;

    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_synced_at", nullable = false)
    private OffsetDateTime lastSyncedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    protected Petition() {
    }

    public Petition(
            String pttId,
            String eraco,
            String pttNo,
            String title,
            long agreeCount,
            LocalDate receivedAt,
            String linkUrl,
            OffsetDateTime now) {
        this.pttId = pttId;
        this.eraco = eraco;
        this.pttNo = pttNo;
        this.title = title;
        this.agreeCount = agreeCount;
        this.receivedAt = receivedAt;
        this.linkUrl = linkUrl;
        this.status = STATUS_COLLECTING;
        this.firstSeenAt = now;
        this.lastSyncedAt = now;
    }

    public String getPttId() {
        return pttId;
    }

    public String getEraco() {
        return eraco;
    }

    public String getPttNo() {
        return pttNo;
    }

    public String getTitle() {
        return title;
    }

    public long getAgreeCount() {
        return agreeCount;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getCommitteeName() {
        return committeeName;
    }

    public LocalDate getCommitteeReferredAt() {
        return committeeReferredAt;
    }

    public Float getAchvRatio() {
        return achvRatio;
    }

    public OffsetDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void refresh(long agreeCount, OffsetDateTime now) {
        this.agreeCount = agreeCount;
        this.lastSyncedAt = now;
    }

    public void close(
            String outcome, String committeeName, LocalDate committeeReferredAt, Float achvRatio, OffsetDateTime now) {
        this.status = STATUS_CLOSED;
        this.outcome = outcome;
        this.committeeName = committeeName;
        this.committeeReferredAt = committeeReferredAt;
        this.achvRatio = achvRatio;
        this.lastSyncedAt = now;
        this.closedAt = now;
    }
}
