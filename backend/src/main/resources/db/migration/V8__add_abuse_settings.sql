CREATE TABLE abuse_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    multi_account_cluster_size INT NOT NULL DEFAULT 4,
    multi_account_trust_penalty_multiplier REAL NOT NULL DEFAULT 0.3,
    trust_maturity_hours INT NOT NULL DEFAULT 72,
    trust_min_weight REAL NOT NULL DEFAULT 0.3,
    duplicate_similarity_threshold REAL NOT NULL DEFAULT 0.85,
    duplicate_lookback_count INT NOT NULL DEFAULT 10,
    duplicate_lookback_minutes INT NOT NULL DEFAULT 1440,
    spike_window_minutes INT NOT NULL DEFAULT 10,
    spike_vote_threshold INT NOT NULL DEFAULT 30,
    spike_reaction_threshold INT NOT NULL DEFAULT 30,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO abuse_settings (id) VALUES (1);

CREATE INDEX idx_anon_users_ip_hash_recent ON anon_users (ip_hash_recent);
CREATE INDEX idx_comments_anon_id_created_at ON comments (anon_id, created_at);
