import type { UserAvailabilityResponse, UserResponse } from "../types/user";
import { setAxiosError } from "../util/api";
import { api } from "./axiosSetup";

export const getCurrentUser = async (setErrorMessage: (message: string) => void): Promise<UserResponse | null> => {
    try{
        const response = await api.get("/users/current");
        if(response.status == 403) return null;
        return response.data as UserResponse;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const searchByUsername = async (username: string, setErrorMessage: (message: string) => void): Promise<Array<UserResponse>> => {
    try{
        const response = await api.get(`/users/username/match-all?username=${username}`);
        return response.data as Array<UserResponse>;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const searchByUsernameAvailability = async(username: string, setErrorMessage: (message: string) => void): Promise<Array<UserAvailabilityResponse>> => {
    try{
        const response = await api.get(`/users/username/match-all-availability?username=${username}`);
        return response.data as Array<UserAvailabilityResponse>;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}