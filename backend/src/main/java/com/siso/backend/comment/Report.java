package com.siso.backend.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(nullable = false)
    private String reason;

    private String detail;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Report() {
    }

    public Report(Comment comment, UUID anonId, String reason, String detail, OffsetDateTime createdAt) {
        this.comment = comment;
        this.anonId = anonId;
        this.reason = reason;
        this.detail = detail;
        this.status = "pending";
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Comment getComment() {
        return comment;
    }
}
