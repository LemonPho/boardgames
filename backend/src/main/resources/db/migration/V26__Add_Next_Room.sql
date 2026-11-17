-- A finished room can point at the room its table moved on to ("Return to room"), so
-- clients still on the old room's final scoreboard can follow it.
ALTER TABLE rooms ADD COLUMN next_room_id UUID NULL REFERENCES rooms(id) ON DELETE SET NULL;
