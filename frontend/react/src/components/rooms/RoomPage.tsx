import { Outlet } from "react-router-dom";
import { RoomContextProvider, useRoomContext } from "../../context/RoomContext";
import WaitingRoom from "./waiting-room/WaitingRoom";
import GameSession from "./GameSession";
import FinalScoreboard from "./FinalScoreboard";
import Cancelled from "./Cancelled";

export default function RoomPage(){

    return(
        <RoomContextProvider>
            <RoomView/>
        </RoomContextProvider>
    );
}

function RoomView() {
  const { room, loading } = useRoomContext();

  if (loading) return null;
  if (!room) return null;

  switch (room.status) {
    case "WAITING": return <WaitingRoom />;
    case "IN_PROGRESS": return <GameSession />;
    case "COMPLETED": return <FinalScoreboard />;
    case "CANCELLED": return <Cancelled />;
  }
}