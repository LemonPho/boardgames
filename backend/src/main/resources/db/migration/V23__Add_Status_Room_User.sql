-- Membership state for a room player. Now that inviting creates a RoomUser up
-- front, we need to tell an invited-but-not-joined player from an active one
-- (and remember declines, to curb re-invite spam):
--   PENDING_INVITE : invited, not yet accepted
--   ACTIVE         : joined and playing (admin, accepted invitee, anonymous)
--   DECLINED       : declined the invite (kept so re-invites can be blocked)
-- Existing rows are all joined players, so they backfill to ACTIVE.
ALTER TABLE rooms_users ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
