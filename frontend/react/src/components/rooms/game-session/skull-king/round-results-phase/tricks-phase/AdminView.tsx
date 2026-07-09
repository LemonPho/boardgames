import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";

export default function AdminView() {
  const { room } = useRoomContext();
  const { trickResults } = useSkullKingSessionContext();

  if (!room) return null;

  return (
    <div className="p-4">
      <h2 className="text-sm font-semibold text-gray-700 mb-3">Tricks Won</h2>
      <div className="flex flex-col gap-2">
        {room.players.map((player) => {
          const result = player.team ? trickResults.get(player.team.id) : null;
          return (
            <div key={player.id} className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3">
              <span className="text-sm text-gray-800">{player.displayName}</span>
              <span className="text-sm font-medium">{result?.tricksWon ?? "—"}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
