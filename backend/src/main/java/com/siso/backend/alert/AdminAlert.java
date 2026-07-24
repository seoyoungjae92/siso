package com.siso.backend.alert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "admin_alerts")
public class AdminAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AdminAlert() {
    }

    public AdminAlert(String type, Map<String, Object> payload, OffsetDateTime createdAt) {
        this.type = type;
        this.payload = payload;
        this.resolved = false;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public boolean isResolved() {
        return resolved;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void resolve() {
        this.resolved = true;
    }
}
