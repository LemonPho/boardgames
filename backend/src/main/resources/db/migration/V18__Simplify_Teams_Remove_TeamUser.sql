DROP TABLE "teams_users";

ALTER TABLE "rooms_users" ADD COLUMN team_id UUID REFERENCES teams(id);

ALTER TABLE "teams" ALTER COLUMN name DROP NOT NULL;

ALTER TABLE "teams" DROP CONSTRAINT uq_session_name;
