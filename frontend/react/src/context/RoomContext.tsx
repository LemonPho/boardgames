import React, { createContext, useContext, useState, useEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { RoomResponse } from "../types/rooms";
import { getRoom } from "../api/rooms";
import { useAlertsContext } from "./AlertsContext";

interface RoomContextType {
    room: RoomResponse | null;
    loading: boolean;
}

const RoomContext = createContext<RoomContextType | null>(null);

export function RoomContextProvider({ children }: { children: React.ReactNode }) {
    const { setErrorMessage } = useAlertsContext();
    const { name } = useParams();
    const navigate = useNavigate();

    const [room, setRoom] = useState<RoomResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const stompClientRef = useRef<Client | null>(null);

    const redirectToStatus = (fetchedRoom: RoomResponse) => {
        const base = `/rooms/${fetchedRoom.name}`;
        switch (fetchedRoom.status) {
            case "WAITING":     return navigate(`${base}/waiting`, { replace: true });
            case "IN_PROGRESS": return navigate(`${base}/in-progress`, { replace: true });
            case "COMPLETED":   return navigate(`${base}/final`, { replace: true });
            case "CANCELLED":   return navigate("/", { replace: true });
        }
    };

    const handleRoomUpdate = (updatedRoom: RoomResponse) => {
        setRoom(updatedRoom);
        redirectToStatus(updatedRoom);
    };

    const connectWebSocket = (roomName: string) => {
        const client = new Client({
            webSocketFactory: () => new SockJS("/ws"),
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
                    redirectToStatus(fetchedRoom);
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

    return (
        <RoomContext.Provider value={{ room, loading }}>
            {children}
        </RoomContext.Provider>
    );
}

export function useRoomContext() {
    const ctx = useContext(RoomContext);
    if (!ctx) throw new Error("Room context failed");
    return ctx;
}