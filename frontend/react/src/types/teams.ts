import type { RoomUserResponse } from "./rooms";

export interface TeamResponse{
  id: string,
  name: string | null,
  player: RoomUserResponse,
  finalScore: number,
  winner: boolean,
  createdAt: Date
}