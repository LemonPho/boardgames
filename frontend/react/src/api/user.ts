import type { UserAvailabilityResponse, UserResponse } from "../types/user";
import type { MatchHistoryResponse } from "../types/matches";
import { setAxiosError } from "../util/api";
import { reviveDates } from "../util/dates";
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

export const getUserByUsername = async (username: string, setErrorMessage: (message: string) => void): Promise<UserResponse | null> => {
    try{
        const response = await api.get(`/users/${username}`);
        return response.data as UserResponse;
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const getMatchHistory = async (username: string, setErrorMessage: (message: string) => void): Promise<Array<MatchHistoryResponse>> => {
    try{
        const response = await api.get(`/users/${username}/matches`);
        const matches = response.data as Array<MatchHistoryResponse>;
        // Revive the ISO date string into a real Date so callers get Date objects.
        return matches.map((match) => reviveDates(match, "playedAt"));
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

export const updateUsername = async (currentUsername: string, username: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await api.put(`/users/${currentUsername}/username`, { username });
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const updateEmail = async (username: string, email: string, currentPassword: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await api.put(`/users/${username}/email`, { email, currentPassword });
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const updatePassword = async (username: string, currentPassword: string, newPassword: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await api.put(`/users/${username}/password`, { currentPassword, newPassword });
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

export const deleteAccount = async (username: string, setErrorMessage: (message: string) => void): Promise<void> => {
    try{
        await api.delete(`/users/${username}`);
    } catch(error) {
        setAxiosError(error, setErrorMessage);
        throw error;
    }
}

