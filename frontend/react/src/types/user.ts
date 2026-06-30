export interface UserResponse {
    username: string,
    email: string
}

export interface UserAvailabilityResponse extends UserResponse{
    inGame: boolean;
}