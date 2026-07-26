ALTER TABLE moderation_settings
    ADD COLUMN classification_model VARCHAR(200) NOT NULL DEFAULT 'openrouter/free';
