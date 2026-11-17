import { useRoomContext } from "../../../../../../context/RoomContext";
import { useRoundData } from "../../../../../../context/RoundDataContext";
import AdminView from "./AdminView";
import PlayerView from "./PlayerView";

export default function TricksPhase() {
  const { currentPlayer, room } = useRoomContext();
  // Not the role directly: an admin in Player view (SELF rooms) gets the player layout.
  const { showAdminView } = useRoundData();

  if (!currentPlayer || !room) return null;

  return (
    <div className="p-4 max-w-md mx-auto">
      {showAdminView ? (
        <AdminView />
      ) : (
        <PlayerView />
      )}
    </div>
  );
}
