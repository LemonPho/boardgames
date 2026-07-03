-- Migration: add ON DELETE CASCADE to session/team event relationships

BEGIN;

-- teams.session_id -> sessions.id
ALTER TABLE teams
    DROP CONSTRAINT teams_session_id_fkey,
    ADD CONSTRAINT teams_session_id_fkey
        FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE;

ALTER TABLE sessions
    DROP CONSTRAINT sessions_room_id_fkey,
    ADD CONSTRAINT sessions_room_id_fkey
        FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE;

-- session_events.session_id -> sessions.id
ALTER TABLE session_events
    DROP CONSTRAINT session_events_session_id_fkey,
    ADD CONSTRAINT session_events_session_id_fkey
        FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE;

-- team_session_events.session_id -> sessions.id
ALTER TABLE team_session_events
    DROP CONSTRAINT team_session_events_session_id_fkey,
    ADD CONSTRAINT team_session_events_session_id_fkey
        FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE;

-- team_session_events.session_event_id -> session_events.id
ALTER TABLE team_session_events
    DROP CONSTRAINT team_session_events_session_event_id_fkey,
    ADD CONSTRAINT team_session_events_session_event_id_fkey
        FOREIGN KEY (session_event_id) REFERENCES session_events(id) ON DELETE CASCADE;

-- team_session_events.team_id -> teams.id
ALTER TABLE team_session_events
    DROP CONSTRAINT team_session_events_team_id_fkey,
    ADD CONSTRAINT team_session_events_team_id_fkey
        FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE;

COMMIT;