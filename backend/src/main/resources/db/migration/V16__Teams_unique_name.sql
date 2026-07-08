ALTER TABLE "teams"
    ADD CONSTRAINT uq_session_name UNIQUE (name, session_id);