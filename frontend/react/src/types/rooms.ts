import type { SimpleGameResponse } from "./games";
import type { TeamResponse } from "./teams";
import type { UserResponse } from "./user";

export type RoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"; 
export type TrackingMode = "ADMIN" | "SELF";
export type RoomUserRole = "ADMIN" | "ANONYMOUS" | "PLAYER";

export interface RoomUserResponse {
    id: string,
    user: UserResponse,
    displayName: string,
    role: RoomUserRole,
    team: TeamResponse | null,
    joinedAt: Date
}

export interface RoomResponse {
    name: string,
    game: SimpleGameResponse,
    status: RoomStatus,
    trackingMode: TrackingMode,
    players: Array<RoomUserResponse>,
    invitations: Array<RoomInvitationResponse>,
    startedAt: Date,
    endedAt: Date,
    createdAt: Date
}

export interface RoomInvitationResponse {
    username: string
    status: string
}

export interface CreateRoomRequest {
    gameName: string | undefined,
    trackingMode: TrackingMode
}

export interface InvitationErrorResponse {
    inGame: string,
    verified: string,
    invited: string
}