import { useRoomContext } from "../../../../../context/RoomContext"
import { useSessionContext } from "../../../../../context/SessionContext";
import { useSkullSkingSessionContext } from "../../../../../context/SkullKingSessionContext";
import AdminView from "./AdminView";
import PlayerView from "./PlayerView";

export default function BidPhase(){
  const { currentPlayer, room } = useRoomContext();
  const { currentSessionEvent } = useSessionContext();
  const { round, cardCount } = useSkullSkingSessionContext();

  if(!currentPlayer || !room || !currentSessionEvent) return null;

  const isAdmin = currentPlayer.role == "ADMIN";


  return(
    <div className="p-4 max-w-md mx-auto">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500">Round {round} · {cardCount} cards</span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Bids</h2>
      {isAdmin ? (
        <AdminView/>
      ) : (
        <PlayerView/>
      )}
    </div>
  );
}