package com.siso.backend.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Side side;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "feed_url")
    private String feedUrl;

    @Column(name = "crawl_type", nullable = false)
    private CrawlType crawlType;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Source() {
    }

    public Source(
            String name,
            Side side,
            String baseUrl,
            String feedUrl,
            CrawlType crawlType,
            OffsetDateTime createdAt) {
        this.name = name;
        this.side = side;
        this.baseUrl = baseUrl;
        this.feedUrl = feedUrl;
        this.crawlType = crawlType;
        this.enabled = true;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Side getSide() {
        return side;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public CrawlType getCrawlType() {
        return crawlType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(String name, Side side, String baseUrl, String feedUrl, CrawlType crawlType) {
        this.name = name;
        this.side = side;
        this.baseUrl = baseUrl;
        this.feedUrl = feedUrl;
        this.crawlType = crawlType;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
