import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";

export default function AdminView() {
  const { room } = useRoomContext();
  const { bonusPoints } = useSkullKingSessionContext();

  if (!room) return null;

  return (
    <div className="p-4">
      <h2 className="text-sm font-semibold text-gray-700 mb-3">Bonus Points</h2>
      <div className="flex flex-col gap-2">
        {room.players.map((player) => {
          const bonus = player.team ? bonusPoints.get(player.team.id) : null;
          return (
            <div key={player.id} className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3">
              <span className="text-sm text-gray-800">{player.displayName}</span>
              <div className="text-sm font-medium">
                {bonus ? `${bonus.points} (${bonus.source})` : "—"}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
