import type { RoomResponse, RoomUserResponse } from "../../../../../types/rooms";

/**
 * Whether the current user may edit the given team's values (bids, tricks, bonus).
 * The admin is the room authority and may edit every team regardless of tracking
 * mode. In self-tracked rooms, other players may set only their own team.
 */
export function canEditTeam(
  room: RoomResponse,
  currentPlayer: RoomUserResponse,
  teamId: string,
): boolean {
  // The admin can always edit any team's values, in either tracking mode.
  if (currentPlayer.role === "ADMIN") {
    return true;
  }
  // Admin-tracked rooms only ever let the admin edit; non-admins get nothing.
  if (room.trackingMode === "ADMIN") {
    return false;
  }
  // Self-tracked: a player may edit their own team.
  return currentPlayer.team?.id === teamId;
}
