CREATE TABLE "teams_users" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id),
    room_user_id UUID NOT NULL REFERENCES rooms_users(id),
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_teams_users UNIQUE (team_id, room_user_id)
);