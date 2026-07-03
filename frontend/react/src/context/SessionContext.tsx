import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import type { SessionResponse } from "../types/sessions";
import { useAlertsContext } from "./AlertsContext";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { createSession } from "../api/sessions";
import { useRoomContext } from "./RoomContext";

interface SessionContextType {
  session: SessionResponse | null;
  handleCreateSession: () => void;
  loading: boolean;
}

const SESSION_UPDATED = "SESSION_UPDATED"

type SessionTopicMessage =
  | { type: "SESSION_UPDATED"; payload: SessionResponse }
//| { type: "SESSION_EVENT"; payload: SessionEventDto }
//| { type: "TEAM_SESSION_EVENT"; payload: TeamSessionEventDto };

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();
  const { room } = useRoomContext();

  const [session, setSession] = useState<SessionResponse | null>(null);
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
              setSession(parsed.payload)
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
      const createdSession = await createSession(room.name, setErrorMessage);
      connectWebSocket();
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
    <SessionContext.Provider value={{ session, handleCreateSession, loading }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSessionContext() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("Session context failed");
  return ctx;
}