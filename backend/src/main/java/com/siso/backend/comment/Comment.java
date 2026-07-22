package com.siso.backend.comment;

import com.siso.backend.pair.TopicPair;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pair_id")
    private TopicPair pair;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String body;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    private String stance;

    @Column(nullable = false)
    private String status;

    @Column(name = "up_count", nullable = false)
    private Integer upCount;

    @Column(name = "down_count", nullable = false)
    private Integer downCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Comment() {
    }

    public Comment(
            TopicPair pair,
            Comment parent,
            UUID anonId,
            String nickname,
            String body,
            String ipHash,
            String stance,
            OffsetDateTime createdAt) {
        this.pair = pair;
        this.parent = parent;
        this.anonId = anonId;
        this.nickname = nickname;
        this.body = body;
        this.ipHash = ipHash;
        this.stance = stance;
        this.status = "visible";
        this.upCount = 0;
        this.downCount = 0;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public TopicPair getPair() {
        return pair;
    }

    public Comment getParent() {
        return parent;
    }

    public UUID getAnonId() {
        return anonId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getBody() {
        return body;
    }

    public String getStance() {
        return stance;
    }

    public String getStatus() {
        return status;
    }

    public Integer getUpCount() {
        return upCount;
    }

    public Integer getDownCount() {
        return downCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
