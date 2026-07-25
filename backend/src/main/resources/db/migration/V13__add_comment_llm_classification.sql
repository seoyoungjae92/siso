ALTER TABLE comments
    ADD COLUMN llm_verdict VARCHAR(20) CHECK (llm_verdict IN ('obvious_violation', 'ambiguous')),
    ADD COLUMN llm_reason TEXT,
    ADD COLUMN llm_classified_at TIMESTAMPTZ;
