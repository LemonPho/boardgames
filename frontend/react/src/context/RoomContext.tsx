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

  const handleRoomUpdate = (updatedRoom: RoomResponse) => {
    setRoom(updatedRoom);
  };

  const connectWebSocket = (roomName: string) => {
    const client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`
      },
      onConnect: () => {
        client.subscribe(`/topic/rooms/${roomName}`, (message) => {
          const updatedRoom = JSON.parse(message.body) as RoomResponse;
          handleRoomUpdate(updatedRoom);
        });
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
    <RoomContext.Provider value={{ room, currentPlayer, loading }}>
      {children}
    </RoomContext.Provider>
  );
}

export function useRoomContext() {
  const ctx = useContext(RoomContext);
  if (!ctx) throw new Error("Room context failed");
  return ctx;
}