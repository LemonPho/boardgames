import type { TeamResponse } from "./teams";

export interface SessionResponse {
  room: string,
  teams: Array<TeamResponse>,
  status: string,
  createdAt: Date,
  endedAt: Date
}

export interface SessionEventResponse {
  type: string,
  sequence: number,
  payload: {},
  createdAt: Date
}

export interface TeamSessionEventResponse{

}


export const SESSION_UPDATED = "SESSION_UPDATED";
export const SESSION_EVENT = "SESSION_EVENT";
export const TEAM_SESSION_EVENT = "TEAM_SESSION_EVENT";

export interface SessionTopicMessage{
  type: typeof SESSION_UPDATED 
      | typeof SESSION_EVENT
      | typeof TEAM_SESSION_EVENT,

  payload:  SessionResponse
          | SessionEventResponse
          | TeamSessionEventResponse 
}