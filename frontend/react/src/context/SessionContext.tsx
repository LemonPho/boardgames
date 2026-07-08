import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import { SESSION_EVENT, SESSION_UPDATED, type SessionEventResponse, type SessionResponse, type SessionTopicMessage } from "../types/sessions";
import { useAlertsContext } from "./AlertsContext";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { createSession } from "../api/sessions";
import { useRoomContext } from "./RoomContext";

interface SessionContextType {
  session: SessionResponse | null;
  currentSessionEvent: SessionEventResponse | null;
  handleCreateSession: () => void;
  loading: boolean;
}

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();
  const { room } = useRoomContext();

  const [session, setSession] = useState<SessionResponse | null>(null);
  const [currentSessionEvent, setCurrentSessionEvent] = useState<SessionEventResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const stompClientRef = useRef<Client | null>(null);

  const connectWebSocket = () => {
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      onConnect: () => {
        client.subscribe(`/topic/sessions/${room?.name}`, (message) => {
          const parsed = JSON.parse(message.body) as SessionTopicMessage;
          console.log(parsed);
          switch (parsed.type) {
            case SESSION_UPDATED:
              setSession(parsed.payload as SessionResponse);
              break;
            case SESSION_EVENT:
              setCurrentSessionEvent(parsed.payload as SessionEventResponse);
              break;
          }
        });
      }
    });

    client.activate();
    stompClientRef.current = client;
  };

  const handleCreateSession = async (): Promise<void> => {
    if(!room) return;

    try{
      setLoading(true);
      connectWebSocket();
      const createdSession = await createSession(room.name, setErrorMessage);
      setSession(createdSession);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!room?.name) return;

    return () => {
      stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    }
  }, [room?.name]);

  return (
    <SessionContext.Provider value={{ session, currentSessionEvent, handleCreateSession, loading }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSessionContext() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("Session context failed");
  return ctx;
}