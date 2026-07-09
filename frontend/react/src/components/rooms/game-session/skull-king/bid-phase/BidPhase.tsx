import { useRoomContext } from "../../../../../context/RoomContext"
import { useSessionContext } from "../../../../../context/SessionContext";
import AdminView from "./AdminView";
import PlayerView from "./PlayerView";

export default function BidPhase(){
  const { currentPlayer, room } = useRoomContext();
  const { currentSessionEvent } = useSessionContext();

  if(!currentPlayer || !room || !currentSessionEvent) return null;

  const isAdmin = currentPlayer.role == "ADMIN";


  return(
    <div className="p-4 max-w-md mx-auto">
      {isAdmin ? (
        <AdminView/>
      ) : (
        <PlayerView/>
      )}
    </div>
  );
}