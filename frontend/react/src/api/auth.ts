import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { auth } from "./constants";

export const verifyEmail = async (token: string): Promise<string> => {
    const response = await auth.get(`/auth/verify?token=${token}`);
    return response.data;
}

export const register = async (data: RegisterRequest): Promise<string> => {
    const response = await auth.post("/register", data);
    return response.data;
}

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await auth.post("/login", data);
    return response.data;
}

export const logout = async (): Promise<boolean> => {
    const response = await auth.post("/logout");
    if(response.status == 200){
        return true;
    } else {
        return false;
    }
}