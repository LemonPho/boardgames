import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { auth } from "./axiosSetup";
import { setAxiosError } from "../util/api";
import type { LoginErrors, RegisterErrors } from "../types/components-types/auth";
import axios from "axios";

export const verifyEmail = async (token: string, setErrorMessage: (message: string) => void): Promise<string> => {
    try{
        const response = await auth.get(`/verify?token=${token}`);
        if(response.status != 200){
            setErrorMessage("Verification failed");
            throw new Error("Verification failed");
        }
        return response.data;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
    
}

export const register = async (data: RegisterRequest, setErrors: (errors: RegisterErrors | null) => void, setErrorMessage: (message: string) => void): Promise<string> => {
    try{
        const response = await auth.post("/register", data);
        return response.data;
    } catch(error) {
        setAxiosError(error, setErrorMessage);

        if(axios.isAxiosError<RegisterErrors>(error) && error.response?.data){
            const registerErrors: RegisterErrors = {
                username: error.response.data.username,
                email: error.response.data.email,
                password: error.response.data.password
            }

            setErrors(registerErrors);
        }
        throw error;
    }
    
}

export const login = async (data: LoginRequest, setErrors: (errors: LoginErrors | null) => void, setErrorMessage: (message: string) => void): Promise<AuthResponse> => {
    try{
        const response = await auth.post("/login", data);
        return response.data;
    } catch(error) {
        if(axios.isAxiosError<LoginErrors>(error) && error.response?.data){
            const loginErrors: LoginErrors = {
                userExists: error.response.data.userExists,
                password: error.response.data.password
            }

            setErrors(loginErrors);
        }
        setAxiosError(error, setErrorMessage);
        throw error;
    }
    
}

export const refresh = async(): Promise<AuthResponse> => {
    const response = await auth.post("/refresh");
    return response.data;
}

export const logout = async (setErrorMessage: (message: string) => void): Promise<boolean> => {
    try{
        const response = await auth.post("/logout");
        if(response.status == 200){
            return true;
        } else {
            return false;
        }
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
    
}

export const csrf = async (setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await auth.get("/csrf");
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}