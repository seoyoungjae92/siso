CREATE TABLE crawl_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    match_similarity_threshold REAL NOT NULL DEFAULT 0.5,
    prune_similarity_threshold REAL NOT NULL DEFAULT 0.5,
    min_cluster_size INT NOT NULL DEFAULT 3,
    grace_period_hours INT NOT NULL DEFAULT 48,
    display_window_days INT NOT NULL DEFAULT 7,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO crawl_settings (id) VALUES (1);
