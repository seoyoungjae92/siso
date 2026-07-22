CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('left', 'right')),
    base_url VARCHAR(500) NOT NULL,
    feed_url VARCHAR(500),
    crawl_type VARCHAR(10) NOT NULL CHECK (crawl_type IN ('rss', 'html')),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE posts (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT NOT NULL REFERENCES sources (id),
    title VARCHAR(500) NOT NULL,
    summary VARCHAR(200) NOT NULL,
    origin_url VARCHAR(1000) NOT NULL UNIQUE,
    origin_url_hash CHAR(64) NOT NULL UNIQUE,
    published_at TIMESTAMPTZ,
    collected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    keywords TEXT[]
);

CREATE INDEX idx_posts_source_id ON posts (source_id);

CREATE TABLE topic_pairs (
    id BIGSERIAL PRIMARY KEY,
    left_post_id BIGINT NOT NULL REFERENCES posts (id),
    right_post_id BIGINT NOT NULL REFERENCES posts (id),
    similarity REAL,
    matched_by VARCHAR(10) NOT NULL CHECK (matched_by IN ('auto', 'manual')),
    status VARCHAR(10) NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'hidden')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    pair_id BIGINT REFERENCES topic_pairs (id),
    post_id BIGINT REFERENCES posts (id),
    parent_id BIGINT REFERENCES comments (id),
    anon_id UUID NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    body TEXT NOT NULL,
    ip_hash CHAR(64) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'visible' CHECK (status IN ('visible', 'blinded', 'deleted')),
    up_count INT NOT NULL DEFAULT 0,
    down_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    pair_id BIGINT NOT NULL REFERENCES topic_pairs (id),
    anon_id UUID NOT NULL,
    stance VARCHAR(10) NOT NULL CHECK (stance IN ('left', 'right', 'neutral')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (pair_id, anon_id)
);

CREATE TABLE reactions (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments (id),
    anon_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('up', 'down')),
    UNIQUE (comment_id, anon_id)
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL REFERENCES comments (id),
    anon_id UUID NOT NULL,
    reason VARCHAR(10) NOT NULL CHECK (reason IN ('abuse', 'hate', 'spam', 'etc')),
    detail TEXT,
    status VARCHAR(10) NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE anon_users (
    anon_id UUID PRIMARY KEY,
    first_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen TIMESTAMPTZ NOT NULL DEFAULT now(),
    ip_hash_recent CHAR(64),
    trust_score REAL NOT NULL DEFAULT 0.5,
    comment_count INT NOT NULL DEFAULT 0,
    vote_count INT NOT NULL DEFAULT 0
);

CREATE TABLE admin_alerts (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    payload JSONB,
    resolved BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
