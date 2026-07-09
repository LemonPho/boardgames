import { useState } from "react";
import { useRoomContext } from "../../../../../context/RoomContext";
import { useSessionContext } from "../../../../../context/SessionContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { AdminBidCard } from "../shared/AdminBidCard";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { session } = useSessionContext();
  const { bids, round, cardCount } = useSkullKingSessionContext();

  if (!room || !currentPlayer || !session) return null;

  const adminTracking = room.trackingMode == "ADMIN";

  const [drafts, setDrafts] = useState<Map<string, number>>(new Map());

  const getDraft = (teamId: string): number => {
    if (drafts.has(teamId)) return drafts.get(teamId)!;
    return bids.get(teamId)?.bid ?? 0;
  }

  const handleIncrement = (teamId: string): void => {
    const current = getDraft(teamId);
    if (current >= cardCount) return;
    setDrafts(prev => new Map(prev).set(teamId, current + 1));
  }

  const handleDecrement = (teamId: string): void => {
    const current = getDraft(teamId);
    if (current <= 0) return;
    setDrafts(prev => new Map(prev).set(teamId, current - 1));
  }

  const handleSubmit = async (teamId: string): Promise<void> => {
    const bid = getDraft(teamId);
    // TODO: API call to submit bid for this team
    console.log("submit bid", teamId, bid);
  }

  const isModified = (teamId: string): boolean => {
    if (!drafts.has(teamId)) return false;
    const submitted = bids.get(teamId);
    if (!submitted) return drafts.get(teamId)! !== 0;
    return drafts.get(teamId)! !== submitted.bid;
  }

  return (
    <div className="p-4">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Bids</h2>
      <div className="grid grid-cols-2 gap-3 mb-4">
        {session.teams.map((team) => (
          <AdminBidCard
            key={team.id}
            playerName={team.player.displayName}
            bid={getDraft(team.id)}
            submitted={bids.has(team.id) && !isModified(team.id)}
            editable={adminTracking}
            onIncrement={() => handleIncrement(team.id)}
            onDecrement={() => handleDecrement(team.id)}
            onSubmit={() => handleSubmit(team.id)}
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
