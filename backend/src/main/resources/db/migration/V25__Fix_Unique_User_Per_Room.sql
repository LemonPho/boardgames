-- The constraint added in V12 was named for "one membership per user per room"
-- but was written against display_name, so it did the opposite of its name:
--   * it did NOT prevent two rooms_users rows for the same user in a room, and
--   * it DID reject two harmless cases — two anonymous players sharing a label
--     ("Dad" twice at a family table), and an anonymous player whose label
--     happens to match a real player's username.
-- Both rejected cases surfaced as an unhandled 500, since display_name is never
-- a lookup key: every operation resolves players by UUID and the name is only
-- ever rendered. Repoint it at the column it was always meant to guard.
--
-- user_id is NULL for anonymous players, and Postgres treats NULLs as distinct
-- in a unique index, so any number of anonymous players per room stays legal.
ALTER TABLE rooms_users DROP CONSTRAINT IF EXISTS unique_user_per_room;
ALTER TABLE rooms_users ADD CONSTRAINT unique_user_per_room UNIQUE (room_id, user_id);
