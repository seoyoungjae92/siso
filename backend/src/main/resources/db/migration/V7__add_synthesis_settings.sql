ALTER TABLE crawl_settings
    ADD COLUMN synthesis_limit INT NOT NULL DEFAULT 10,
    ADD COLUMN synthesis_model VARCHAR(200) NOT NULL DEFAULT 'openrouter/free';
