package com.motomutterers.boardgames.rooms.model.Room;

/**
 * Membership state of a RoomUser. Inviting a real player creates the RoomUser
 * immediately as PENDING_INVITE; accepting flips it to ACTIVE; declining marks
 * it DECLINED (retained so the admin can't re-invite/spam a user who said no).
 * The admin and anonymous players are ACTIVE from creation.
 */
public enum RoomUserStatus {
    PENDING_INVITE,
    ACTIVE,
    DECLINED
}
