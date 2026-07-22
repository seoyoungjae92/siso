CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE posts ADD COLUMN embedding vector(768);

CREATE INDEX idx_posts_embedding ON posts USING hnsw (embedding vector_cosine_ops);
