CREATE TABLE feedback (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(10) NOT NULL CHECK (category IN ('suggestion', 'report', 'bug', 'etc')),
    body TEXT NOT NULL,
    contact VARCHAR(200),
    anon_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'new' CHECK (status IN ('new', 'resolved')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedback_status_created_at ON feedback (status, created_at DESC);
