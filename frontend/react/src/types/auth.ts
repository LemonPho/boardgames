export interface RegisterRequest{
    email: string,
    username: string,
    password: string
}

export interface LoginRequest{
    isUsername: boolean,
    primaryKey: string,
    password: string
}

export interface AuthResponse{
    accessToken: string,
}

// Result of POST /auth/google. Either the sign-in completed (an account already
// existed) or this Google identity is new and needs a username first, in which
// case registrationToken carries the verified identity to /auth/google/register.
export interface GoogleAuthResponse{
    registrationRequired: boolean,
    accessToken: string | null,
    registrationToken: string | null,
    email: string | null
}

export interface CsrfResponse {
    headerName: string,
    parameterName: string,
    token: string
}

export interface AuthProps{
    tab: "login" | "register"
}