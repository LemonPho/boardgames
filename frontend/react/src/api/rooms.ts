import type { CreateRoomRequest, RoomInvitationResponse, RoomResponse, RoomUserResponse } from "../types/rooms";
import { setAxiosError } from "../util/api";
import { api } from "./axiosSetup";
import type { UserAvailabilityResponse } from "../types/user";

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

export const invitePlayerToRoom = async (username: string, roomName: string, setErrorMessage: (message: string) => void): Promise<RoomInvitationResponse> => {
  try {
    const response = await api.post(`/rooms/${roomName}/invite`, {
      username: username,
    });

    return response.data as RoomInvitationResponse;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const revokeRoomInvite = async (username: string, roomName: string, setErrorMessage: (message: string) => void): Promise<Array<RoomInvitationResponse>> => {
  try{
    const response = await api.post(`/rooms/${roomName}/revoke-invite`, {
      username: username
    });
    return response.data as Array<RoomInvitationResponse>;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const createAnonymousPlayer = async (displayName: string, roomName: string, setErrorMessage: (message: string) => void): Promise<RoomUserResponse> => {
  try{
    const response = await api.post(`/rooms/${roomName}/create-anonymous`, {
      displayName: displayName
    });

    return response.data as RoomUserResponse;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const removePlayer = async (displayName: string, roomName: string, setErrorMessage: (message: string) => void): Promise<RoomUserResponse[]> => {
  try{
    const response = await api.post(`/rooms/${roomName}/remove-player`, {
      displayName: displayName
    });

    return response.data as RoomUserResponse[];
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const searchUsersAvailability = async (username: string, roomName: string, setErrorMessage: (message: string) => void): Promise<Array<UserAvailabilityResponse>> => {
  try {
    const response = await api.get(`/rooms/${roomName}/search-users?username=${username}`);
    return response.data as Array<UserAvailabilityResponse>;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const acceptInvite = async (token: string, setErrorMessage: (message: string) => void): Promise<string> => {
  try{
    const response = await api.put(`/rooms/accept?token=${token}`);
    return response.data;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const cancelRoom = async (roomName: string, setErrorMessage: (message: string) => void): Promise<string> => {
  try{
    const response = await api.put(`/rooms/${roomName}/cancel`);
    return response.data;
  } catch(error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}
