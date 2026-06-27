import type { CreateRoomRequest, RoomResponse, TrackingMode } from "../types/rooms";
import { setAxiosError } from "../util/api";
import { api } from "./axiosSetup";

export const createRoom = async (request: CreateRoomRequest, setErrorMessage: (message: string) => void): Promise<RoomResponse | null> => {
  try {
    const response = await api.post("/rooms", {
      trackingMode: request.trackingMode,
      gameName: request.gameName
    });

    const room = response.data as RoomResponse;
    return room;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const getRoom = async (roomName: string, setErrorMessage: (message: string) => void): Promise<RoomResponse | null> => {
  try{
    const response = await api.get(`/rooms/${roomName}`);
    const room = response.data as RoomResponse;
    return room;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}