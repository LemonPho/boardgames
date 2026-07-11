import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitBonusPoints, finishRound } from "../../../../../../api/skullKing";
import { EMPTY_BONUS, bonusEligibility, type TeamBonus } from "../../../../../../types/skull-king";
import { useDebouncedSave } from "../../../../../../util/useDebouncedSave";
import { BonusCard } from "../../shared/BonusCard";
import { canEditTeam } from "../../shared/permissions";

const sameBonus = (a: TeamBonus | undefined, b: TeamBonus | undefined): boolean =>
  JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [drafts, setDrafts] = useState<Map<string, TeamBonus>>(new Map());

  if (!room || !currentPlayer || !state) return null;

  const bids = state.bids ?? {};
  const trickResults = state.trickResults ?? {};
  const bonuses = state.bonuses ?? {};

  const eligibility = (teamId: string) => bonusEligibility(bids[teamId], trickResults[teamId]);

  const getValue = (teamId: string): TeamBonus =>
    drafts.get(teamId) ?? bonuses[teamId] ?? EMPTY_BONUS;

  const status = (teamId: string): "saving" | "saved" | null => {
    if (drafts.has(teamId) && !sameBonus(drafts.get(teamId), bonuses[teamId])) return "saving";
    if (teamId in bonuses) return "saved";
    return null;
  };

  const change = (teamId: string, next: TeamBonus): void => {
    setDrafts(prev => new Map(prev).set(teamId, next));
    schedule(teamId, () => submitBonusPoints(room.name, teamId, next, setErrorMessage));
  };

  const handleFinishRound = async (): Promise<void> => {
    try {
      // Record any team not yet saved (ineligible teams save an empty record), flush, then finish.
      const pending = state.teams.filter((team) => !(team.id in bonuses) && !drafts.has(team.id));
      for (const team of pending) {
        const bonus = eligibility(team.id).eligible ? getValue(team.id) : EMPTY_BONUS;
        await submitBonusPoints(room.name, team.id, bonus, setErrorMessage);
      }
      await flush();
      await finishRound(room.name, setErrorMessage);
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
      <h2 className="text-lg font-medium text-center mb-6">Bonus points</h2>

      <div className="flex flex-col gap-3 mb-4">
        {state.teams.map((team) => {
          const { eligible, reason } = eligibility(team.id);
          return (
            <BonusCard
              key={team.id}
              playerName={team.player.displayName}
              bonus={getValue(team.id)}
              eligible={eligible}
              ineligibleReason={reason}
              editable={canEditTeam(room, currentPlayer, team.id)}
              status={status(team.id)}
              onChange={(next) => change(team.id, next)}
            />
          );
        })}
      </div>

      <button
        type="button"
        onClick={handleFinishRound}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        {state.round >= 10 ? "Finish game" : "Finish round"}
      </button>
    </div>
  );
}
