ALTER TABLE crawl_settings
    ADD COLUMN detail_fetch_limit INT NOT NULL DEFAULT 20;
