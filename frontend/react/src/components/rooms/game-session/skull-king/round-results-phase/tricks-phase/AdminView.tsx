import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitTrickResult, startBonusPoints } from "../../../../../../api/skullKing";
import { useDebouncedSave } from "../../../../../../util/useDebouncedSave";
import { AdminCounterCard } from "../../shared/AdminCounterCard";
import { canEditTeam } from "../../shared/permissions";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [drafts, setDrafts] = useState<Map<string, number>>(new Map());

  if (!room || !currentPlayer || !state) return null;

  const trickResults = state.trickResults ?? {};

  const getValue = (teamId: string): number => {
    if (drafts.has(teamId)) return drafts.get(teamId)!;
    return trickResults[teamId] ?? 0;
  };

  const status = (teamId: string): "saving" | "saved" | null => {
    if (drafts.has(teamId) && drafts.get(teamId) !== trickResults[teamId]) return "saving";
    if (teamId in trickResults) return "saved";
    return null;
  };

  const change = (teamId: string, next: number): void => {
    if (next < 0 || next > state.cardCount) return;
    setDrafts(prev => new Map(prev).set(teamId, next));
    schedule(teamId, () => submitTrickResult(room.name, teamId, next, setErrorMessage));
  };

  const handleStartBonusPoints = async (): Promise<void> => {
    try {
      const pending = state.teams.filter((team) => !(team.id in trickResults) && !drafts.has(team.id));
      for (const team of pending) {
        await submitTrickResult(room.name, team.id, getValue(team.id), setErrorMessage);
      }
      await flush();
      await startBonusPoints(room.name, setErrorMessage);
    } catch {
      return;
    }
  };

  const allResultsSubmitted = state.teams.every((team) => team.id in trickResults || drafts.has(team.id));

  return (
    <div className="p-4">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {state.round} · {state.cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Tricks won</h2>
      <div className="grid grid-cols-2 gap-3 mb-4">
        {state.teams.map((team) => (
          <AdminCounterCard
            key={team.id}
            playerName={team.player.displayName}
            editable={canEditTeam(room, currentPlayer, team.id)}
            team={team}
            serverValue={trickResults[team.id] ?? 0}
            max={state.cardCount}
            status={status(team.id)}
            change={change}
          />
        ))}
      </div>

      <button
        type="button"
        onClick={handleStartBonusPoints}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        Continue to bonus points
      </button>
    </div>
  );
}
