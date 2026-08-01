import type { SimpleGameResponse } from "./games";
import type { TeamResponse } from "./teams";
import type { UserResponse } from "./user";

export type RoomStatus = "WAITING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
export type TrackingMode = "ADMIN" | "SELF";
export type RoomUserRole = "ADMIN" | "ANONYMOUS" | "PLAYER";
// Membership state. Pending invites live in the same players list as active
// players (declined ones are hidden by the backend).
export type RoomUserStatus = "PENDING_INVITE" | "ACTIVE" | "DECLINED";

// Per-room game configuration (JSONB on the backend; expandable for other games).
export interface RoomConfiguration {
    trackingMode: TrackingMode,
    advancedCards: boolean
}

export interface RoomUserResponse {
    id: string,
    user: UserResponse,
    displayName: string,
    role: RoomUserRole,
    status: RoomUserStatus,
    // Seat/turn order within the room; also drives the game's rotation.
    playingPosition: number,
    team: TeamResponse | null,
    joinedAt: Date
}

export interface RoomResponse {
    name: string,
    game: SimpleGameResponse,
    status: RoomStatus,
    trackingMode: TrackingMode,
    configuration: RoomConfiguration,
    // Includes both active players and pending invites (see RoomUserStatus).
    players: Array<RoomUserResponse>,
    startedAt: Date,
    endedAt: Date,
    createdAt: Date
}

export interface CreateRoomRequest {
    gameName: string | undefined,
    configuration: RoomConfiguration
}

export interface InvitationErrorResponse {
    inGame: string,
    verified: string,
    invited: string
}