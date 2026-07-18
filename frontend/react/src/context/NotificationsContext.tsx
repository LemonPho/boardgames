import { createContext, useContext, useState, useEffect, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { NotificationResponse } from "../types/notifications";
import { getNotifications } from "../api/notifications";
import { useAlertsContext } from "./AlertsContext";
import { useUserContext } from "./UserContext";
import { useAuthenticationContext } from "./AuthenticationContext";

interface NotificationsContextType {
  notifications: NotificationResponse[];
  unreadCount: number;
  removeNotification: (id: string) => void;
  refresh: () => void;
}

const NotificationsContext = createContext<NotificationsContextType | null>(null);

export function NotificationsContextProvider({ children }: { children: React.ReactNode }) {
  const { user } = useUserContext();
  const { accessToken } = useAuthenticationContext();
  const { setErrorMessage } = useAlertsContext();

  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const stompClientRef = useRef<Client | null>(null);

  const refresh = async (): Promise<void> => {
    try {
      const data = await getNotifications(setErrorMessage);
      setNotifications(data);
    } catch {
      /* surfaced via alerts */
    }
  };

  // Prepend a pushed notification, guarding against a duplicate id (e.g. if a
  // fetch and a push race).
  const addNotification = (incoming: NotificationResponse): void => {
    setNotifications((prev) =>
      prev.some((n) => n.id === incoming.id) ? prev : [incoming, ...prev]
    );
  };

  const removeNotification = (id: string): void => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  // Seed the list and open the live subscription once we have an authenticated
  // user. Re-runs if the user or token changes (login/logout).
  useEffect(() => {
    if (!user || !accessToken) {
      setNotifications([]);
      stompClientRef.current?.deactivate();
      stompClientRef.current = null;
      return;
    }

    refresh();

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      onConnect: () => {
        client.subscribe(`/topic/notifications/${user.username}`, (message) => {
          const incoming = JSON.parse(message.body) as NotificationResponse;
          addNotification(incoming);
        });
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    };
  }, [user?.username, accessToken]);

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <NotificationsContext.Provider
      value={{ notifications, unreadCount, removeNotification, refresh }}
    >
      {children}
    </NotificationsContext.Provider>
  );
}

export function useNotificationsContext() {
  const ctx = useContext(NotificationsContext);
  if (!ctx) throw new Error("Notifications context failed");
  return ctx;
}
