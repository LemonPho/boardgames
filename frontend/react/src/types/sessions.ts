import type { TeamResponse } from "./teams";

export interface SessionResponse {
  id: string,
  room: string,
  teams: Array<TeamResponse>,
  status: string,
  createdAt: Date,
  endedAt: Date
}
