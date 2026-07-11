import { useRoomContext } from "../../../../../../context/RoomContext";
import AdminView from "./AdminView";
import PlayerView from "./PlayerView";

export default function BonusPointsPhase() {
  const { currentPlayer, room } = useRoomContext();

  if (!currentPlayer || !room) return null;

  const isAdmin = currentPlayer.role == "ADMIN";

  return (
    <div className="p-4 max-w-md mx-auto">
      {isAdmin ? (
        <AdminView />
      ) : (
        <PlayerView />
      )}
    </div>
  );
}
