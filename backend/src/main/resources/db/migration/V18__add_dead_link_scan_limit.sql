ALTER TABLE crawl_settings
    ADD COLUMN dead_link_scan_limit INT NOT NULL DEFAULT 100;
