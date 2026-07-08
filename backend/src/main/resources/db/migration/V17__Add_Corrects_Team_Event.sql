ALTER TABLE team_session_events
    ADD COLUMN corrects_team_session_event_id UUID REFERENCES team_session_events(id);