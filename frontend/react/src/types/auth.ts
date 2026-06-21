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
    refreshToken: string
}