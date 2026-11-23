import { createContext, useContext, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useRoomContext } from "./RoomContext";
import { useAuthenticationContext } from "./AuthenticationContext";
import { useAlertsContext } from "./AlertsContext";
import { getSkullKingState } from "../api/skullKing";
import type { SkullKingState, TeamBonus, VersionConflict } from "../types/skull-king";

interface SkullKingSessionContextType {
  state: SkullKingState | null;
  loading: boolean;
  // Re-fetch the current state from the server and apply it. Normally state
  // arrives over the WebSocket, but a dropped push can leave the client on a
  // stale phase — callers use this to re-sync. Returns the state (null on error).
  refreshState: () => Promise<SkullKingState | null>;
  // Silently adopt one team's current server value + version (from a 409), without waiting
  // for a push or refetch.
  applyServerValue: (conflict: VersionConflict) => void;
}

const SkullKingContext = createContext<SkullKingSessionContextType | null>(null);

export function SkullKingContextProvider({ children }: { children: React.ReactNode }) {
  const { room, currentPlayer } = useRoomContext();
  const { accessToken } = useAuthenticationContext();
  const { setErrorMessage } = useAlertsContext();

  const [state, setState] = useState<SkullKingState | null>(null);
  const [loading, setLoading] = useState(true);
  // Latest token, read on every (re)connect: the access token is short-lived, so a
  // reconnect with the token captured at first connect would be rejected. Reading it
  // through a ref also keeps a token refresh from tearing the socket down (and losing
  // the pushes sent during the gap).
  // (Updated in an effect rather than during render; reconnects always happen later.)
  const tokenRef = useRef(accessToken);
  useEffect(() => {
    tokenRef.current = accessToken;
  }, [accessToken]);

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

  // Patches only the conflicting team's entry; the next push or refresh replaces the whole
  // state anyway. Functional update so it applies to the newest state, not a captured one.
  // Sets (or, when the server has no row yet, removes) one team's entry. A 409 against a
  // missing row (e.g. the round was reset) carries only the version, no value fields.
  const withEntry = <T,>(map: Record<string, T> | undefined, teamId: string, value: T | undefined): Record<string, T> => {
    const next = { ...(map ?? {}) };
    if (value === undefined) delete next[teamId];
    else next[teamId] = value;
    return next;
  };

  const applyServerValue = (conflict: VersionConflict): void => {
    setState(prev => {
      if (!prev) return prev;
      const { teamId, version } = conflict;
      switch (conflict.field) {
        case "BIDS":
          return {
            ...prev,
            bids: withEntry(prev.bids, teamId, conflict.bid),
            bidVersions: { ...prev.bidVersions, [teamId]: version },
          };
        case "TRICK_RESULTS":
          return {
            ...prev,
            trickResults: withEntry(prev.trickResults, teamId, conflict.tricksWon),
            trickVersions: { ...prev.trickVersions, [teamId]: version },
          };
        case "BONUS_POINTS": {
          // All six fields come together, or none (no row yet).
          const bonus: TeamBonus | undefined = conflict.standardFourteens === undefined ? undefined : {
            standardFourteens: conflict.standardFourteens,
            blackFourteen: conflict.blackFourteen ?? false,
            mermaidsByPirate: conflict.mermaidsByPirate ?? 0,
            piratesBySkullKing: conflict.piratesBySkullKing ?? 0,
            skullKingByMermaid: conflict.skullKingByMermaid ?? false,
            loot: conflict.loot ?? 0,
          };
          return {
            ...prev,
            bonuses: withEntry(prev.bonuses, teamId, bonus),
            bonusVersions: { ...prev.bonusVersions, [teamId]: version },
          };
        }
      }
    });
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
      beforeConnect: () => {
        client.connectHeaders = { Authorization: `Bearer ${tokenRef.current}` };
      },
      onConnect: () => {
        client.subscribe(topic, (message) => {
          setState(JSON.parse(message.body) as SkullKingState);
        });
        // Pushes sent while disconnected are lost, so resync on every (re)connect. Same as
        // refreshState(), inlined so this closure reads the effect's own roomName and the
        // effect doesn't depend on a function that's recreated every render.
        getSkullKingState(roomName, () => {}).then(setState).catch(() => {});
      }
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  // accessToken is deliberately absent: tokenRef supplies a fresh one on each reconnect.
  }, [roomName, role, teamId]);

  return (
    <SkullKingContext.Provider value={{ state, loading, refreshState, applyServerValue }}>
      {children}
    </SkullKingContext.Provider>
  );
}

export function useSkullKingSessionContext() {
  const ctx = useContext(SkullKingContext);
  if (!ctx) throw new Error("Skull king context error");
  return ctx;
}
