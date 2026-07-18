package com.motomutterers.boardgames.notifications.dto;

/**
 * Typed data for a ROOM_INVITATION notification. This is both the persisted
 * payload (stored as JSONB) and the `data` object sent to the frontend, so the
 * client renders and acts without parsing an untyped blob:
 *   - roomName          : shown, and the target the Accept action operates on
 *   - gameName          : shown as the subline (e.g. "Skull King")
 *   - roomAdminUsername : who sent the invite ("{admin} invited you…")
 *   - token             : lets the client accept via the existing invite-accept flow
 */
public record RoomInvitationPayload(
    String roomName,
    String gameName,
    String roomAdminUsername,
    String token
) {}
