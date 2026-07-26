package com.siso.backend.newsletter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers")
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    protected NewsletterSubscriber() {
    }

    public NewsletterSubscriber(String email, UUID token, OffsetDateTime createdAt) {
        this.email = email;
        this.status = "pending";
        this.token = token;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return status;
    }

    public UUID getToken() {
        return token;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void confirm(OffsetDateTime now) {
        this.status = "confirmed";
        this.confirmedAt = now;
    }

    /** 재구독 시 pending으로 되돌리고 새 토큰을 발급 — 이전 토큰은 무효화된다. */
    public void resetForResubscribe(UUID newToken) {
        this.status = "pending";
        this.token = newToken;
        this.confirmedAt = null;
    }
}
