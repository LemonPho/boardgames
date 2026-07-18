// A completed match from a user's perspective, for the profile match history.
export interface MatchHistoryResponse {
  sessionId: string;
  game: string;
  roomName: string;
  placement: number;
  players: number;
  score: number;
  won: boolean;
  playedAt: Date;
}
