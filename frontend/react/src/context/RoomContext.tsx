import { createContext, useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { RoomResponse, RoomUserResponse } from "../types/rooms";
import { getRoom } from "../api/rooms";
import { useAlertsContext } from "./AlertsContext";
import { useUserContext } from "./UserContext";
import { useAuthenticationContext } from "./AuthenticationContext";
import { createUpdateWaiter } from "../util/expectUpdate";

interface RoomContextType {
  room: RoomResponse | null;
  currentPlayer: RoomUserResponse | null;
  loading: boolean;
  // Re-fetch the room over REST now. Used after a failed mutation, where our view may be
  // stale (e.g. the player was already removed).
  refreshRoom: () => Promise<void>;
  // Runs a room mutation with expect-or-refresh: on success, rely on the room push
  // (REST-refresh if none arrives in 2 s, counting a push that raced ahead of the
  // response); on failure, resync now since our view may be stale. Rethrows failures.
  syncRoomMutation: (action: () => Promise<void>) => Promise<void>;
}

const RoomContext = createContext<RoomContextType | null>(null);

export function RoomContextProvider({ children }: { children: React.ReactNode }) {
  const { user } = useUserContext();
  const { setErrorMessage } = useAlertsContext();
  const { accessToken } = useAuthenticationContext();
  const { name } = useParams();

  const [room, setRoom] = useState<RoomResponse | null>(null);
  const [currentPlayer, setCurrentPlayer] = useState<RoomUserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const stompClientRef = useRef<Client | null>(null);
  // Latest token, read on every (re)connect — the access token is short-lived, so a
  // reconnect with the token captured at first connect would be rejected.
  const tokenRef = useRef(accessToken);
  tokenRef.current = accessToken;
  // One waiter per provider: every room push satisfies all pending expectations.
  const waiter = useRef(createUpdateWaiter());

  const handleRoomUpdate = (updatedRoom: RoomResponse) => {
    setRoom(updatedRoom);
  };

  const refreshRoom = async (): Promise<void> => {
    if (!name) return;
    try {
      const fetchedRoom = await getRoom(name, () => {});
      if (fetchedRoom) setRoom(fetchedRoom);
    } catch {
      // Best-effort resync; the triggering action surfaces its own errors.
    }
  };

  const syncRoomMutation = async (action: () => Promise<void>): Promise<void> => {
    const since = waiter.current.mark();
    try {
      await action();
      waiter.current.expect(refreshRoom, since);
    } catch (error) {
      await refreshRoom();
      throw error;
    }
  };

  const connectWebSocket = (roomName: string) => {
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      beforeConnect: () => {
        client.connectHeaders = { Authorization: `Bearer ${tokenRef.current}` };
      },
      onConnect: () => {
        client.subscribe(`/topic/rooms/${roomName}`, (message) => {
          const updatedRoom = JSON.parse(message.body) as RoomResponse;
          handleRoomUpdate(updatedRoom);
          waiter.current.notify();
        });
        // Pushes sent while disconnected are lost, so resync on every (re)connect.
        refreshRoom();
      },
      onDisconnect: () => {
        console.log("WebSocket disconnected");
      }
    });

    client.activate();
    stompClientRef.current = client;
  };

  useEffect(() => {
    if (!name) return;
    const roomWaiter = waiter.current;

    const fetchData = async () => {
      setLoading(true);
      try {
        const fetchedRoom = await getRoom(name, setErrorMessage);
        if (fetchedRoom) {
          setRoom(fetchedRoom);
          connectWebSocket(fetchedRoom.name);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchData();

    return () => {
      stompClientRef.current?.deactivate();
      // Drop pending fallbacks so a left room isn't re-fetched after the fact.
      roomWaiter.notify();
    };
  }, [name]);

  useEffect(() => {
    if (!room || !user) return;

    const me = room.players.find((p) =>
      p.role != "ANONYMOUS" && p.user?.username === user.username
    );

    // Clear when the user isn't in this room (e.g. after navigating to a recreated room
    // they were skipped from, or being removed) so a stale membership from the
    // previous room is never shown.
    setCurrentPlayer(me ?? null);
  }, [room, user])

  return (
    <RoomContext.Provider value={{ room, currentPlayer, loading, refreshRoom, syncRoomMutation }}>
      {children}
    </RoomContext.Provider>
  );
}

export function useRoomContext() {
  const ctx = useContext(RoomContext);
  if (!ctx) throw new Error("Room context failed");
  return ctx;
}

// For components rendered both inside and outside a room (e.g. FinalScoreboard): null when
// there's no RoomContextProvider above.
export function useOptionalRoomContext() {
  return useContext(RoomContext);
}
