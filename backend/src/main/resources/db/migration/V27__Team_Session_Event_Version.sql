-- Optimistic-concurrency version per team value: bumped by human edits (submits and
-- corrections), not by server prefill. Lets the server reject saves made against a stale
-- value and tell the client the current one. Existing rows start at 0, the same version a
-- client assumes when there's no row at all.
ALTER TABLE team_session_events ADD COLUMN version INT NOT NULL DEFAULT 0;
