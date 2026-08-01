ALTER TABLE posts ADD COLUMN topic_pair_id BIGINT REFERENCES topic_pairs (id);
CREATE INDEX idx_posts_topic_pair_id ON posts (topic_pair_id);

-- 기존 1:1 쌍의 좌/우 글을 새 컬럼으로 백필(컬럼 드롭 전에 먼저 실행)
UPDATE posts p SET topic_pair_id = tp.id
FROM topic_pairs tp WHERE p.id = tp.left_post_id OR p.id = tp.right_post_id;

ALTER TABLE topic_pairs DROP COLUMN left_post_id, DROP COLUMN right_post_id;

ALTER TABLE crawl_settings
    ADD COLUMN cohort_similarity_threshold REAL NOT NULL DEFAULT 0.5,
    ADD COLUMN synthesis_min_posts_per_side INT NOT NULL DEFAULT 1;
