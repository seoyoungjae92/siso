ALTER TABLE sources
    ADD COLUMN consecutive_failures INT NOT NULL DEFAULT 0;

ALTER TABLE crawl_settings
    ADD COLUMN source_failure_threshold INT NOT NULL DEFAULT 5;
