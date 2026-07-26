import { createContext, useContext, useEffect, useState } from "react";
import type { UserResponse } from "../types/user";
import { useAlertsContext } from "./AlertsContext";
import { useAuthenticationContext } from "./AuthenticationContext";
import { getCurrentUser } from "../api/user";

interface UserContextType {
  user: UserResponse | null,
  retrieveCurrentUser: () => Promise<void>,
  setUser: (user: UserResponse | null) => void;
}

const UserContext = createContext<UserContextType | null>(null);

export function UserContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();
  const { accessToken } = useAuthenticationContext();

  const [user, setUser] = useState<UserResponse | null>(null);

  const retrieveCurrentUser = async (): Promise<void> => {
    const currentUser = await getCurrentUser(setErrorMessage);
    setUser(currentUser);
  }

  useEffect(() => {
    if (!accessToken) {
      setUser(null);
      return;
    }

    // If the token changes (e.g. logout) while this fetch is in flight, its
    // result must not overwrite the cleared user.
    let cancelled = false;
    (async () => {
      try {
        const currentUser = await getCurrentUser(setErrorMessage);
        if (!cancelled) setUser(currentUser);
      } catch {
        if (!cancelled) setUser(null);
      }
    })();

    return () => { cancelled = true; };
  }, [accessToken]);

  return (
    <UserContext.Provider
      value={{ user, retrieveCurrentUser, setUser }}
    >
      {children}
    </UserContext.Provider>
  )
}

export function useUserContext() {
  const ctx = useContext(UserContext);
  if (!ctx) throw new Error("Application context not loaded");
  return ctx;
}
