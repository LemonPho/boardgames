import type { TeamResponse } from "./teams";

export interface SessionResponse {
  room: string,
  teams: Array<TeamResponse>,
  status: string,
  createdAt: Date,
  endedAt: Date
}

export interface SessionEventResponse {
  id: string,
  type: string,
  sequence: number,
  payload: {},
  createdAt: Date
}

export interface SessionStateResponse {
  session: SessionResponse,
  currentEvent: SessionEventResponse | null
}

export interface TeamSessionEventResponse {
  id: string,
  teamId: string,
  type: "BIDS" | "TRICK_RESULTS" | "BONUS_POINTS",
  sequence: number,
  payload: {},
  correctsEventId: string | null,
  createdAt: Date
}


export const SESSION_UPDATED = "SESSION_UPDATED";
export const SESSION_EVENT = "SESSION_EVENT";

export interface SessionTopicMessage{
  type: typeof SESSION_UPDATED
      | typeof SESSION_EVENT,

  payload:  SessionResponse
          | SessionEventResponse
}