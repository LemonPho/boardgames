import { createContext, useContext, useState, useEffect, useRef } from "react";
import { useParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { RoomResponse, RoomUserResponse } from "../types/rooms";
import { getRoom } from "../api/rooms";
import { useAlertsContext } from "./AlertsContext";
import { useUserContext } from "./UserContext";
import { useAuthenticationContext } from "./AuthenticationContext";

interface RoomContextType {
  room: RoomResponse | null;
  currentPlayer: RoomUserResponse | null;
  loading: boolean;
  // Re-fetch the room over REST. Used after admin mutations so the UI doesn't
  // depend solely on the WebSocket push (which a poor connection can drop).
  refreshRoom: () => Promise<void>;
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
    };
  }, [name]);

  useEffect(() => {
    if (!room || !user) return;

    const me = room.players.find((p) =>
      p.role != "ANONYMOUS" && p.user?.username === user.username
    );

    if (me) {
      setCurrentPlayer(me);
    }
  }, [room, user])

  return (
    <RoomContext.Provider value={{ room, currentPlayer, loading, refreshRoom }}>
      {children}
    </RoomContext.Provider>
  );
}

export function useRoomContext() {
  const ctx = useContext(RoomContext);
  if (!ctx) throw new Error("Room context failed");
  return ctx;
}