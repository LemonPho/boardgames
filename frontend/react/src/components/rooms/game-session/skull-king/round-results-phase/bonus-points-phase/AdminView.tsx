import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitBonusPoints, finishRound } from "../../../../../../api/skullKing";
import { EMPTY_BONUS, bonusEligibility, type TeamBonus } from "../../../../../../types/skull-king";
import { BonusCard } from "../../shared/BonusCard";

export default function AdminView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  const [drafts, setDrafts] = useState<Map<string, TeamBonus>>(new Map());

  if (!room || !currentPlayer || !state) return null;

  const adminTracking = room.trackingMode == "ADMIN";
  const bids = state.bids ?? {};
  const trickResults = state.trickResults ?? {};
  const bonuses = state.bonuses ?? {};

  const eligibility = (teamId: string) => bonusEligibility(bids[teamId], trickResults[teamId]);

  const getDraft = (teamId: string): TeamBonus =>
    drafts.get(teamId) ?? bonuses[teamId] ?? EMPTY_BONUS;

  const setDraft = (teamId: string, next: TeamBonus): void => {
    setDrafts(prev => new Map(prev).set(teamId, next));
  };

  const clearDraft = (teamId: string): void => {
    setDrafts(prev => {
      const map = new Map(prev);
      map.delete(teamId);
      return map;
    });
  };

  const submitTeam = async (teamId: string): Promise<void> => {
    // An ineligible team (missed bid, or made a zero bid) submits an empty record.
    const bonus = eligibility(teamId).eligible ? getDraft(teamId) : EMPTY_BONUS;
    await submitBonusPoints(room.name, teamId, bonus, setErrorMessage);
    clearDraft(teamId);
  };

  const handleFinishRound = async (): Promise<void> => {
    // Submit any teams not yet recorded, then finish. Stop on the first failure.
    try {
      const pending = state.teams.filter((team) => !(team.id in bonuses));
      for (const team of pending) {
        await submitTeam(team.id);
      }
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
              bonus={getDraft(team.id)}
              eligible={eligible}
              ineligibleReason={reason}
              submitted={team.id in bonuses}
              editable={adminTracking}
              onChange={(next) => setDraft(team.id, next)}
              onSubmit={() => submitTeam(team.id)}
            />
          );
        })}
      </div>

      <button
        type="button"
        disabled={!adminTracking && !state.teams.every((team) => team.id in bonuses)}
        onClick={handleFinishRound}
        className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
      >
        {state.round >= 10 ? "Finish game" : "Finish round"}
      </button>
    </div>
  );
}
