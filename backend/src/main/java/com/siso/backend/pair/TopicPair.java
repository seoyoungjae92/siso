package com.siso.backend.pair;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "topic_pairs")
public class TopicPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Float similarity;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    private String title;

    @Column(name = "left_stance")
    private String leftStance;

    @Column(name = "right_stance")
    private String rightStance;

    protected TopicPair() {
    }

    public Long getId() {
        return id;
    }

    public Float getSimilarity() {
        return similarity;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public String getTitle() {
        return title;
    }

    public String getLeftStance() {
        return leftStance;
    }

    public String getRightStance() {
        return rightStance;
    }
}
