// Final standings for a session, keyed by room name. Teams are pre-ranked
// (1st = highest score) by the backend.
export interface ScoreboardTeam {
  teamId: string;
  playerName: string | null;
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
