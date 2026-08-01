import type { AuthResponse, GoogleAuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { auth } from "./axiosSetup";
import { setAxiosError } from "../util/api";
import type { GoogleRegisterErrors, LoginErrors, RegisterErrors } from "../types/components-types/auth";
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

// Exchanges the Google ID token ("credential") for either an access token or a
// registration token — see GoogleAuthResponse.
export const googleLogin = async (credential: string, setErrorMessage: (message: string) => void): Promise<GoogleAuthResponse> => {
    try{
        const response = await auth.post("/google/login", { credential });
        return response.data;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

// Completes a first-time Google sign-in with the username the player chose.
export const googleRegister = async (
    registrationToken: string,
    username: string,
    setErrors: (errors: GoogleRegisterErrors | null) => void,
    setErrorMessage: (message: string) => void
): Promise<AuthResponse> => {
    try{
        const response = await auth.post("/google/register", { registrationToken, username });
        return response.data;
    } catch(error) {
        if(axios.isAxiosError<GoogleRegisterErrors>(error) && error.response?.data){
            setErrors({ username: error.response.data.username });
        }
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const forgotPassword = async (isUsername: boolean, primaryKey: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await auth.post("/forgot-password", { isUsername, primaryKey });
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const resetPassword = async (token: string, newPassword: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await auth.post("/reset-password", { token, newPassword });
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const refresh = async(): Promise<AuthResponse> => {
    const response = await auth.post("/refresh");
    return response.data;
}

export const logout = async (): Promise<boolean> => {
    // Logout is best-effort from the client's view: even if the server call
    // fails (e.g. the session/refresh cookie is already gone), we still clear
    // local auth state. So swallow errors rather than surfacing them.
    try{
        await auth.post("/logout");
    } catch {
        /* ignore — we clear client state regardless */
    }
    return true;
}

export const csrf = async (setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await auth.get("/csrf");
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}