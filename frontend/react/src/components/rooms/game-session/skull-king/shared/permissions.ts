import type { RoomResponse, RoomUserResponse } from "../../../../../types/rooms";
import type { TeamResponse } from "../../../../../types/teams";

/**
 * Whether the current user may edit the given team's values (bids, tricks, bonus).
 * Admin-tracked rooms let the admin set every team; self-tracked rooms let each
 * player — including the admin, who also plays — set only their own team, plus
 * anonymous players' teams (they can't log in to submit for themselves, so the
 * admin enters their points).
 */
export function canEditTeam(
  room: RoomResponse,
  currentPlayer: RoomUserResponse,
  teamId: string,
  teams: TeamResponse[],
): boolean {
  if (room.trackingMode === "ADMIN") {
    return currentPlayer.role === "ADMIN";
  }
  if (currentPlayer.team?.id === teamId) {
    return true;
  }
  // In self-tracked rooms the admin still covers anonymous players.
  if (currentPlayer.role === "ADMIN") {
    const team = teams.find((t) => t.id === teamId);
    return team?.player?.role === "ANONYMOUS";
  }
  return false;
}
