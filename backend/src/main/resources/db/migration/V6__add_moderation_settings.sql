CREATE TABLE moderation_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    auto_blind_report_threshold INT NOT NULL DEFAULT 20,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO moderation_settings (id) VALUES (1);
