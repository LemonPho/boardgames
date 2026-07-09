import type { SessionResponse, SessionStateResponse } from "../types/sessions";
import { setAxiosError } from "../util/api"
import { api } from "./axiosSetup";

export const createSession = async (roomName: string, setErrorMessage: (message: string) => void): Promise<SessionResponse> => {
  try{
    const response = await api.post("/sessions", {
      roomName
    });

    return response.data as SessionResponse;
  } catch(error){
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const getSessionState = async (roomName: string, setErrorMessage: (message: string) => void): Promise<SessionStateResponse> => {
  try{
    const response = await api.get(`/sessions/${roomName}`);
    return response.data as SessionStateResponse;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}