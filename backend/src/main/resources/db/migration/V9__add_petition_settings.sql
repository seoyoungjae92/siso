CREATE TABLE petition_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    eraco VARCHAR(20) NOT NULL DEFAULT '제22대',
    top_n INT NOT NULL DEFAULT 10,
    window_days INT NOT NULL DEFAULT 30,
    cache_ttl_minutes INT NOT NULL DEFAULT 30,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO petition_settings (id) VALUES (1);
