import React, { createContext, useContext, useState } from "react";
import { useAlertsContext } from "./AlertsContext";
import { createSession } from "../api/sessions";
import { useRoomContext } from "./RoomContext";

interface SessionContextType {
  handleCreateSession: () => void;
  loading: boolean;
}

const SessionContext = createContext<SessionContextType | null>(null);

export function SessionContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();
  const { room } = useRoomContext();

  const [loading, setLoading] = useState<boolean>(false);

  const handleCreateSession = async (): Promise<void> => {
    if(!room) return;

    try{
      setLoading(true);
      await createSession(room.name, setErrorMessage);
    } finally {
      setLoading(false);
    }
  }

  return (
    <SessionContext.Provider value={{ handleCreateSession, loading }}>
      {children}
    </SessionContext.Provider>
  );
}

export function useSessionContext() {
  const ctx = useContext(SessionContext);
  if (!ctx) throw new Error("Session context failed");
  return ctx;
}
