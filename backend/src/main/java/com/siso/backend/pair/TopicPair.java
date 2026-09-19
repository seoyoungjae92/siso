package com.siso.backend.pair;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    // 보강 필드(V28) — 보강 없는 주제는 네 컬럼 모두 NULL.
    private String background;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "left_points")
    private String[] leftPoints;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "right_points")
    private String[] rightPoints;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "discussion_questions")
    private String[] discussionQuestions;

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

    public String getBackground() {
        return background;
    }

    public String[] getLeftPoints() {
        return leftPoints;
    }

    public String[] getRightPoints() {
        return rightPoints;
    }

    public String[] getDiscussionQuestions() {
        return discussionQuestions;
    }
}
