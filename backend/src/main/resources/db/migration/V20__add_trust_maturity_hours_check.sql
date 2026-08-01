ALTER TABLE abuse_settings
    ADD CONSTRAINT abuse_settings_trust_maturity_hours_positive CHECK (trust_maturity_hours > 0);
