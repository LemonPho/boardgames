import { useState } from "react";
import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../context/AlertsContext";
import { submitBid, startRound } from "../../../../../api/skullKing";
import { AdminCounterCard } from "../shared/AdminCounterCard";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  if (!room || !currentPlayer || !state) return null;

  const adminTracking = room.trackingMode == "ADMIN";
  const bids = state.bids ?? {};

  const [drafts, setDrafts] = useState<Map<string, number>>(new Map());

  const getDraft = (teamId: string): number => {
    if (drafts.has(teamId)) return drafts.get(teamId)!;
    return bids[teamId] ?? 0;
  }

  const handleIncrement = (teamId: string): void => {
    const current = getDraft(teamId);
    if (current >= state.cardCount) return;
    setDrafts(prev => new Map(prev).set(teamId, current + 1));
  }

  const handleDecrement = (teamId: string): void => {
    const current = getDraft(teamId);
    if (current <= 0) return;
    setDrafts(prev => new Map(prev).set(teamId, current - 1));
  }

  const handleSubmit = async (teamId: string): Promise<void> => {
    await submitBid(room.name, teamId, getDraft(teamId), setErrorMessage);
    setDrafts(prev => {
      const next = new Map(prev);
      next.delete(teamId);
      return next;
    });
  }

  const handleStartRound = async (): Promise<void> => {
    // Submit any teams whose bid hasn't been registered yet, then advance.
    // If any submission fails, stop here — the error is already surfaced
    // and we must not advance the phase.
    try {
      const pending = state.teams.filter((team) => !(team.id in bids));
      for (const team of pending) {
        await submitBid(room.name, team.id, getDraft(team.id), setErrorMessage);
      }
      await startRound(room.name, setErrorMessage);
    } catch {
      return;
    }
  }

  const isModified = (teamId: string): boolean => {
    if (!drafts.has(teamId)) return false;
    if (!(teamId in bids)) return drafts.get(teamId)! !== 0;
    return drafts.get(teamId)! !== bids[teamId];
  }

  const allBidsSubmitted = state.teams.every((team) => team.id in bids);

  return (
    <div className="p-4">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {state.round} · {state.cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Bids</h2>
      <div className="grid grid-cols-2 gap-3 mb-4">
        {state.teams.map((team) => (
          <AdminCounterCard
            key={team.id}
            playerName={team.player.displayName}
            value={getDraft(team.id)}
            submitted={team.id in bids && !isModified(team.id)}
            editable={adminTracking}
            onIncrement={() => handleIncrement(team.id)}
            onDecrement={() => handleDecrement(team.id)}
            onSubmit={() => handleSubmit(team.id)}
          />
        ))}
      </div>

      <button
        type="button"
        disabled={!adminTracking && !allBidsSubmitted}
        onClick={handleStartRound}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        Start round
      </button>
    </div>
  );
}
