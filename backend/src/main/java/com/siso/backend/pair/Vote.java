package com.siso.backend.pair;

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
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_id", nullable = false)
    private TopicPair pair;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(nullable = false)
    private String stance;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Vote() {
    }

    public Vote(TopicPair pair, UUID anonId, String stance, OffsetDateTime updatedAt) {
        this.pair = pair;
        this.anonId = anonId;
        this.stance = stance;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getAnonId() {
        return anonId;
    }

    public String getStance() {
        return stance;
    }

    public void update(String stance, OffsetDateTime updatedAt) {
        this.stance = stance;
        this.updatedAt = updatedAt;
    }
}
