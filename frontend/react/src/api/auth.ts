import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { api } from "./constants";

export const verifyEmail = async (token: string): Promise<string> => {
    const response = await api.get(`/auth/verify?token=${token}`);
    return response.data;
}

export const register = async (data: RegisterRequest): Promise<string> => {
    const response = await api.post("/auth/register", data);
    return response.data;
}

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await api.post("/auth/login", data);
    return response.data;
}