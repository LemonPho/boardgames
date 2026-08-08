// What kind of player a row represents: a live account (links to its profile), a
// deleted account (name only — the profile is gone), or an anonymous placeholder
// that never had an account.
export type ScoreboardPlayerType = "ACTIVE" | "DELETED" | "ANONYMOUS";

// Final standings for a session, keyed by room name. Teams are pre-ranked
// (1st = highest score) by the backend.
export interface ScoreboardTeam {
  teamId: string;
  playerName: string | null;
  // The account's username for the profile link; null unless playerType is ACTIVE.
  // Distinct from playerName, which is only ever display text.
  username: string | null;
  playerType: ScoreboardPlayerType;
  score: number;
  placement: number;
  won: boolean;
}

export interface ScoreboardResponse {
  roomName: string;
  game: string;
  completed: boolean;
  endedAt: Date | null;
  teams: ScoreboardTeam[];
}
