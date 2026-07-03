import type { SessionResponse } from "../types/sessions";
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

export const getSession = async (sessionName: string, setErrorMessage: (message: string) => void): Promise<SessionResponse> => {
  try{
    const response = await api.get(`/sessions/${sessionName}`);
    return response.data as SessionResponse;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}