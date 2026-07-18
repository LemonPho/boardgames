import type { NotificationResponse } from "../types/notifications";
import { setAxiosError } from "../util/api";
import { api } from "./axiosSetup";

export const getNotifications = async (
  setErrorMessage: (message: string) => void
): Promise<NotificationResponse[]> => {
  try {
    const response = await api.get("/notifications");
    return response.data as NotificationResponse[];
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
};

export const markNotificationRead = async (
  id: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.put(`/notifications/${id}/read`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
};
