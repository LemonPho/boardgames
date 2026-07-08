import type { RoomUserResponse } from "./rooms";

export interface TeamResponse{
  name: string,
  teamUsers: Array<TeamUserResponse>,
  finalScore: number,
  winner: boolean,
  createdAt: Date
}

export interface TeamUserResponse{
  user: RoomUserResponse
}