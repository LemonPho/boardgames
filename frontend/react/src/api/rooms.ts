import axios from "axios";
import type { CreateRoomRequest, InvitationErrorResponse, RoomResponse, TrackingMode } from "../types/rooms";
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
  try {
    const response = await api.get(`/rooms/${roomName}`);
    const room = response.data as RoomResponse;
    return room;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const invitePlayerToRoom = async (username: string, roomName: string, setErrors: (errors: InvitationErrorResponse | null) => void, setErrorMessage: (message: string) => void): Promise<void> => {
  try {
    await api.post(`/rooms/invite`, {
      username: username,
      roomName: roomName,
    });
  } catch (error: unknown) {
    if (axios.isAxiosError(error) && error.response?.data) {
      const data = error.response.data;
      const invitationErrorResponse: InvitationErrorResponse = {
        inGame: data.inGame,
        verified: data.verified,
        invited: data.invited,
      };
      setErrors(invitationErrorResponse);
    }
    throw error;
  }
}
