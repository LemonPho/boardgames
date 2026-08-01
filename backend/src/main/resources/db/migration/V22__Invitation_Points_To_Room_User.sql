-- Repoint invitations at the RoomUser they concern instead of the User directly.
-- Going forward, inviting a player creates their RoomUser immediately (carrying
-- playing_position from invite time), and the token references it. Invite data
-- (token, expires_at, status) stays on the token; membership and seat/order live
-- on rooms_users.
--
-- Existing invitations are discarded: they predate this model and are no longer
-- meaningful (accepting now deletes the token, and starting a game clears any
-- outstanding ones, so there's no lasting backlog to preserve).

-- 1. Drop all existing tokens so the new NOT NULL FK has nothing to backfill.
DELETE FROM room_invitations_tokens;

-- 2. Point at the RoomUser and retire the now-redundant direct user pointer.
--    ON DELETE CASCADE: removing a RoomUser (kick/leave) drops its token too.
ALTER TABLE room_invitations_tokens
    ADD COLUMN room_user_id UUID NOT NULL REFERENCES rooms_users(id) ON DELETE CASCADE;
ALTER TABLE room_invitations_tokens DROP COLUMN user_id;

-- 3. At most one invitation token per RoomUser (re-inviting replaces the old one).
ALTER TABLE room_invitations_tokens ADD CONSTRAINT uq_invitation_room_user UNIQUE (room_user_id);
