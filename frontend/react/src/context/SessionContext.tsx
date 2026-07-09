import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import { SESSION_EVENT, SESSION_UPDATED, type SessionEventResponse, type SessionResponse, type SessionTopicMessage, type TeamSessionEventResponse } from "../types/sessions";
import { useAlertsContext } from "./AlertsContext";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { createSession, getSessionState } from "../api/sessions";
import { useRoomContext } from "./RoomContext";
import { useAuthenticationContext } from "./AuthenticationContext";

interface SessionContextType {
  session: SessionResponse | null;
  currentSessionEvent: SessionEventResponse | null;
  teamEvents: TeamSessionEventResponse[];
  handleCreateSession: () => void;
  loading: boolean;
}

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();
  const { room, currentPlayer } = useRoomContext();
  const { accessToken } = useAuthenticationContext();

  const [session, setSession] = useState<SessionResponse | null>(null);
  const [currentSessionEvent, setCurrentSessionEvent] = useState<SessionEventResponse | null>(null);
  const [teamEvents, setTeamEvents] = useState<TeamSessionEventResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const stompClientRef = useRef<Client | null>(null);

  const connectWebSocket = () => {
    if (stompClientRef.current) return;
    if (!room || !currentPlayer) return;

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      onConnect: () => {
        // Global session events (phase transitions)
        client.subscribe(`/topic/sessions/${room.name}`, (message) => {
          const parsed = JSON.parse(message.body) as SessionTopicMessage;
          switch (parsed.type) {
            case SESSION_UPDATED:
              setSession(parsed.payload as SessionResponse);
              break;
            case SESSION_EVENT:
              setCurrentSessionEvent(parsed.payload as SessionEventResponse);
              break;
          }
        });

        // Per-team or admin subscription
        if (currentPlayer.role === "ADMIN") {
          client.subscribe(`/topic/sessions/${room.name}/admin`, (message) => {
            const event = JSON.parse(message.body) as TeamSessionEventResponse;
            setTeamEvents(prev => [...prev, event]);
          });
        } else if (currentPlayer.team) {
          client.subscribe(`/topic/sessions/${room.name}/teams/${currentPlayer.team.id}`, (message) => {
            const event = JSON.parse(message.body) as TeamSessionEventResponse;
            setTeamEvents(prev => [...prev, event]);
          });
        }
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
    if (!room?.name || !currentPlayer) return;

    if (room.status === "IN_PROGRESS") {
      const fetchSessionState = async () => {
        try {
          setLoading(true);
          const state = await getSessionState(room.name, setErrorMessage);
          setSession(state.session);
          if (state.currentEvent) setCurrentSessionEvent(state.currentEvent);
          connectWebSocket();
        } finally {
          setLoading(false);
        }
      };

      fetchSessionState();
    } else {
      setLoading(false);
    }

    return () => {
      stompClientRef.current?.deactivate();
      stompClientRef.current = null;
    }
  }, [room?.name, currentPlayer]);

  return (
    <SessionContext.Provider value={{ session, currentSessionEvent, teamEvents, handleCreateSession, loading }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSessionContext() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("Session context failed");
  return ctx;
}
