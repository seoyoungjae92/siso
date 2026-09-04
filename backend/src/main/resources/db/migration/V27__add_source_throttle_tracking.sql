ALTER TABLE sources
    ADD COLUMN throttle_strikes INT NOT NULL DEFAULT 0,
    ADD COLUMN throttled_until TIMESTAMPTZ;
