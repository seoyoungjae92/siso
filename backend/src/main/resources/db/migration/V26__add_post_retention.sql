ALTER TABLE crawl_settings
    ADD COLUMN post_retention_days INT NOT NULL DEFAULT 10,
    ADD COLUMN stale_post_scan_limit INT NOT NULL DEFAULT 200;
