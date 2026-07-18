export interface UserResponse {
    username: string,
    email: string,
    createdAt: string
}

export interface UserAvailabilityResponse extends UserResponse{
    inGame: boolean;
    invited: boolean;
}