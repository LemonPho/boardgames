import { useState } from "react";
import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../context/AlertsContext";
import { submitBid, startRound } from "../../../../../api/skullKing";
import { useDebouncedSave } from "../../../../../util/useDebouncedSave";
import { AdminCounterCard } from "../shared/AdminCounterCard";
import { canEditTeam } from "../shared/permissions";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [drafts, setDrafts] = useState<Map<string, number>>(new Map());

  if (!room || !currentPlayer || !state) return null;

  const bids = state.bids ?? {};

  const getValue = (teamId: string): number => {
    if (drafts.has(teamId)) return drafts.get(teamId)!;
    return bids[teamId] ?? 0;
  };

  const status = (teamId: string): "saving" | "saved" | null => {
    if (drafts.has(teamId) && drafts.get(teamId) !== bids[teamId]) return "saving";
    if (teamId in bids) return "saved";
    return null;
  };

  const change = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    setDrafts(prev => new Map(prev).set(teamId, value));
    schedule(teamId, () => submitBid(room.name, teamId, value, setErrorMessage));
  };

  const handleStartRound = async (): Promise<void> => {
    try {
      // Persist any teams still on their default value, flush pending saves, then advance.
      const pending = state.teams.filter((team) => !(team.id in bids) && !drafts.has(team.id));
      for (const team of pending) {
        await submitBid(room.name, team.id, getValue(team.id), setErrorMessage);
      }
      await flush();
      await startRound(room.name, setErrorMessage);
    } catch {
      return;
    }
  };

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
            editable={canEditTeam(room, currentPlayer, team.id)}
            team={team}
            serverValue={bids[team.id] ?? 0}
            max={state.cardCount}
            status={status(team.id)}
            change={change}
          />
        ))}
      </div>

      <button
        type="button"
        onClick={handleStartRound}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        Start round
      </button>
    </div>
  );
}
