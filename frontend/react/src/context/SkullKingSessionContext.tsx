import { createContext, useContext, useEffect, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useRoomContext } from "./RoomContext";
import { useAuthenticationContext } from "./AuthenticationContext";
import { useAlertsContext } from "./AlertsContext";
import { getSkullKingState } from "../api/skullKing";
import type { SkullKingState } from "../types/skull-king";

interface SkullKingSessionContextType {
  state: SkullKingState | null;
  loading: boolean;
  // Re-fetch the current state from the server and apply it. Normally state
  // arrives over the WebSocket, but a dropped push can leave the client on a
  // stale phase — callers use this to re-sync. Returns the state (null on error).
  refreshState: () => Promise<SkullKingState | null>;
}

const SkullKingContext = createContext<SkullKingSessionContextType | null>(null);

export function SkullKingContextProvider({ children }: { children: React.ReactNode }) {
  const { room, currentPlayer } = useRoomContext();
  const { accessToken } = useAuthenticationContext();
  const { setErrorMessage } = useAlertsContext();

  const [state, setState] = useState<SkullKingState | null>(null);
  const [loading, setLoading] = useState(true);

  const roomName = room?.name;
  const role = currentPlayer?.role;
  const teamId = currentPlayer?.team?.id;

  const refreshState = async (): Promise<SkullKingState | null> => {
    if (!roomName) return null;
    try {
      const fetched = await getSkullKingState(roomName, () => {});
      setState(fetched);
      return fetched;
    } catch {
      return null;
    }
  };

  // Fetch the initial state whenever the room changes.
  useEffect(() => {
    if (!roomName) return;

    const fetchState = async () => {
      try {
        setLoading(true);
        const fetched = await getSkullKingState(roomName, setErrorMessage);
        setState(fetched);
      } finally {
        setLoading(false);
      }
    };

    fetchState();
  }, [roomName]);

  // Subscribe to live state updates. Keyed on the concrete values the
  // subscription depends on so it rebuilds cleanly once the team is known.
  useEffect(() => {
    if (!roomName || !role) return;
    if (role !== "ADMIN" && !teamId) return;

    const topic = role === "ADMIN"
      ? `/topic/sessions/${roomName}/admin`
      : `/topic/sessions/${roomName}/teams/${teamId}`;

    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      onConnect: () => {
        client.subscribe(topic, (message) => {
          setState(JSON.parse(message.body) as SkullKingState);
        });
      }
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [roomName, role, teamId, accessToken]);

  return (
    <SkullKingContext.Provider value={{ state, loading, refreshState }}>
      {children}
    </SkullKingContext.Provider>
  );
}

export function useSkullKingSessionContext() {
  const ctx = useContext(SkullKingContext);
  if (!ctx) throw new Error("Skull king context error");
  return ctx;
}
