ALTER TABLE comments ADD COLUMN stance VARCHAR(10) CHECK (stance IN ('left', 'right', 'neutral'));
