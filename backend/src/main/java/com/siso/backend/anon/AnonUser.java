package com.siso.backend.anon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "anon_users")
public class AnonUser {

    @Id
    @Column(name = "anon_id")
    private UUID anonId;

    @Column(name = "first_seen", nullable = false)
    private OffsetDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private OffsetDateTime lastSeen;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ip_hash_recent", length = 64)
    private String ipHashRecent;

    @Column(name = "trust_score", nullable = false)
    private Float trustScore;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "vote_count", nullable = false)
    private Integer voteCount;

    protected AnonUser() {
    }

    public AnonUser(UUID anonId, OffsetDateTime now, String ipHash) {
        this.anonId = anonId;
        this.firstSeen = now;
        this.lastSeen = now;
        this.ipHashRecent = ipHash;
        this.trustScore = 0.5f;
        this.commentCount = 0;
        this.voteCount = 0;
    }

    public void recordComment(OffsetDateTime now, String ipHash) {
        this.lastSeen = now;
        this.ipHashRecent = ipHash;
        this.commentCount += 1;
    }

    public UUID getAnonId() {
        return anonId;
    }

    public Integer getCommentCount() {
        return commentCount;
    }
}
