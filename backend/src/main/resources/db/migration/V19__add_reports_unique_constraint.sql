ALTER TABLE reports
    ADD CONSTRAINT reports_comment_id_anon_id_key UNIQUE (comment_id, anon_id);
