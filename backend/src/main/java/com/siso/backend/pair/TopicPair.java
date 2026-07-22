package com.siso.backend.pair;

import com.siso.backend.post.Post;
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

@Entity
@Table(name = "topic_pairs")
public class TopicPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_post_id", nullable = false)
    private Post leftPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "right_post_id", nullable = false)
    private Post rightPost;

    @Column(nullable = false)
    private Float similarity;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TopicPair() {
    }

    public Long getId() {
        return id;
    }

    public Post getLeftPost() {
        return leftPost;
    }

    public Post getRightPost() {
        return rightPost;
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
}
