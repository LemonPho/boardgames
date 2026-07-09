import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { bonusPoints } = useSkullKingSessionContext();

  if (!currentPlayer || !currentPlayer.team) return null;

  const myBonus = bonusPoints.get(currentPlayer.team.id);

  return (
    <div className="p-4">
      <div className="border border-gray-100 rounded-xl px-4 py-3">
        <span className="text-sm text-gray-800">{currentPlayer.displayName}</span>
        <div className="ml-2 text-sm font-medium inline">
          {myBonus ? `${myBonus.points} (${myBonus.source})` : "—"}
        </div>
      </div>
    </div>
  );
}
