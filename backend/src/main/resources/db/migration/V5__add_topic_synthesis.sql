ALTER TABLE topic_pairs
    ADD COLUMN title VARCHAR(200),
    ADD COLUMN left_stance VARCHAR(500),
    ADD COLUMN right_stance VARCHAR(500);
