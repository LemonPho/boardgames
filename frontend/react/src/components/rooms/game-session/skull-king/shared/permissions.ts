import type { RoomResponse, RoomUserResponse } from "../../../../../types/rooms";

/**
 * Whether the current user may edit the given team's values (bids, tricks, bonus).
 * Admin-tracked rooms let the admin set every team; self-tracked rooms let each
 * player — including the admin, who also plays — set only their own team.
 */
export function canEditTeam(
  room: RoomResponse,
  currentPlayer: RoomUserResponse,
  teamId: string,
): boolean {
  if (room.trackingMode === "ADMIN") {
    return currentPlayer.role === "ADMIN";
  }
  return currentPlayer.team?.id === teamId;
}
