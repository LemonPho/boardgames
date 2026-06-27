import type { SimpleGameResponse } from "./games";
import type { UserResponse } from "./user";

export type RoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"; 
export type TrackingMode = "ADMIN" | "SELF";
export type RoomUserRole = "ADMIN" | "ANONYMOUS" | "PLAYER";

export interface RoomUserResponse {
    user: UserResponse,
    displayName: string,
    role: RoomUserRole,
    joinedAt: Date
}

export interface RoomResponse {
    name: string,
    game: SimpleGameResponse,
    status: RoomStatus,
    trackingMode: TrackingMode,
    users: Array<RoomUserResponse>,
    startedAt: Date,
    endedAt: Date,
    createdAt: Date
}

export interface CreateRoomRequest {
    gameName: string | undefined,
    trackingMode: TrackingMode
}