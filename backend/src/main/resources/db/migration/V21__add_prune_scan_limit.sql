ALTER TABLE crawl_settings
    ADD COLUMN prune_scan_limit INT NOT NULL DEFAULT 100;
