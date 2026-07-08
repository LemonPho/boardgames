import { useState } from "react";
import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullSkingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { AdminBidCard } from "../shared/AdminBidCard";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();

  if (!room || !currentPlayer) return null;

  const adminTracking = room.trackingMode == "ADMIN";

  // local draft state per team, seeded from currentBids when a value first arrives
  const [drafts, setDrafts] = useState<Map<string, number>>(new Map());


  const tempFunction = (variable: string):void => {
    console.log("function");
  }

  return (
    <div className="p-4">
      <div className="grid grid-cols-2 gap-3 mb-4">
        {room.players.map((player) => (
          <AdminBidCard
            key={player.displayName}
            playerName={player.displayName}
            bid={0}
            submitted={false}
            editable={adminTracking || player.displayName == currentPlayer.displayName || player.role == "ANONYMOUS"}
            onIncrement={() => tempFunction(player.displayName)}
            onDecrement={() => tempFunction(player.displayName)}
            onSubmit={() => tempFunction(player.displayName)}
          />
        ))}
      </div>

      <button
        type="button"
        disabled={true}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        Start round
      </button>
    </div>
  );
}