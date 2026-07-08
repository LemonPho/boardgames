import { useRoomContext } from "../../../context/RoomContext";
import { SkullKingContextProvider } from "../../../context/SkullKingSessionContext";
import SkullKingSession from "./skull-king/SkullKingSession";

export default function GameSession() {
  const { room } = useRoomContext();

  if(!room) return;

  if(room.game.name == "Skull King"){
    return(
      <SkullKingContextProvider>
        <SkullKingSession/>
      </SkullKingContextProvider>
    );
  }
}