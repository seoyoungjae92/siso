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

import java.util.UUID;

@Entity
@Table(name = "reactions")
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(nullable = false)
    private String type;

    protected Reaction() {
    }

    public Reaction(Comment comment, UUID anonId, String type) {
        this.comment = comment;
        this.anonId = anonId;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public Comment getComment() {
        return comment;
    }

    public UUID getAnonId() {
        return anonId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
