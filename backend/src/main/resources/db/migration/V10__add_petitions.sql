CREATE TABLE petitions (
    ptt_id VARCHAR(64) PRIMARY KEY,
    eraco VARCHAR(20) NOT NULL,
    ptt_no VARCHAR(20) NOT NULL,
    title TEXT NOT NULL,
    agree_count BIGINT NOT NULL,
    received_at DATE NOT NULL,
    link_url TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'collecting' CHECK (status IN ('collecting', 'closed')),
    outcome VARCHAR(20) CHECK (outcome IN ('established', 'not_established')),
    committee_name VARCHAR(200),
    committee_referred_at DATE,
    achv_ratio REAL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ
);

CREATE INDEX idx_petitions_status_agree_count ON petitions (status, agree_count DESC);
CREATE INDEX idx_petitions_status_received_at ON petitions (status, received_at);

ALTER TABLE petition_settings DROP COLUMN cache_ttl_minutes;
