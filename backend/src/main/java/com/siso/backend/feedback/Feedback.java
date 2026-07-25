package com.siso.backend.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String body;

    private String contact;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Feedback() {
    }

    public Feedback(String category, String body, String contact, UUID anonId, OffsetDateTime createdAt) {
        this.category = category;
        this.body = body;
        this.contact = contact;
        this.anonId = anonId;
        this.status = "new";
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getBody() {
        return body;
    }

    public String getContact() {
        return contact;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void resolve() {
        this.status = "resolved";
    }
}
