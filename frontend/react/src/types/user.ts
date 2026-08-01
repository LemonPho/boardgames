export interface UserResponse {
    username: string,
    email: string,
    createdAt: string,
    // How the account signs in. GOOGLE accounts have no password, so the
    // password and email sections don't apply to them.
    authProvider: "LOCAL" | "GOOGLE"
}

export interface UserAvailabilityResponse extends UserResponse{
    inGame: boolean;
    invited: boolean;
    declined: boolean;
}