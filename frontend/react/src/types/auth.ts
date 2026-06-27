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

export interface CsrfResponse {
    headerName: string,
    parameterName: string,
    token: string
}

export interface AuthProps{
    tab: "login" | "register"
}