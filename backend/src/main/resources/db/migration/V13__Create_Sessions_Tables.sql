CREATE TABLE "sessions" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP
);

CREATE TABLE "teams" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES sessions(id),
    name VARCHAR(32) NOT NULL,
    final_score BIGINT NOT NULL,
    winner BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE "session_events" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES sessions(id),
    type VARCHAR(32) NOT NULL,
    sequence INTEGER NOT NULL,
    payload JSONB,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_session_events_session_id ON session_events(session_id);

CREATE TABLE "team_session_events" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES sessions(id),
    session_event_id UUID NOT NULL REFERENCES session_events(id),
    team_id UUID NOT NULL REFERENCES teams(id),
    type VARCHAR(32) NOT NULL,
    sequence INTEGER NOT NULL,
    payload JSONB,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_team_session_events_sequence UNIQUE (session_id, sequence),
    CONSTRAINT uq_team_session_events_response UNIQUE (session_event_id, team_id)
);

CREATE INDEX idx_team_session_events_session_id ON team_session_events(session_id);
CREATE INDEX idx_team_session_events_session_event_id ON team_session_events(session_event_id);
CREATE INDEX idx_team_session_events_team_id ON team_session_events(team_id);