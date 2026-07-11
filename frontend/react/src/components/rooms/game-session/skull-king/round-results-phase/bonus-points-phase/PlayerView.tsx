import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitBonusPoints } from "../../../../../../api/skullKing";
import { EMPTY_BONUS, bonusEligibility, type TeamBonus } from "../../../../../../types/skull-king";
import { useDebouncedSave } from "../../../../../../util/useDebouncedSave";
import { BonusCard } from "../../shared/BonusCard";

const sameBonus = (a: TeamBonus | undefined, b: TeamBonus | undefined): boolean =>
  JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

export default function PlayerView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule } = useDebouncedSave();

  const [draft, setDraft] = useState<TeamBonus | null>(null);

  if (!room || !currentPlayer || !currentPlayer.team || !state) return null;

  const teamId = currentPlayer.team.id;
  const bids = state.bids ?? {};
  const trickResults = state.trickResults ?? {};
  const bonuses = state.bonuses ?? {};

  const { eligible, reason } = bonusEligibility(bids[teamId], trickResults[teamId]);
  const selfTracking = room.trackingMode === "SELF";

  const serverBonus = bonuses[teamId];
  const current = draft ?? serverBonus ?? EMPTY_BONUS;

  const status = (): "saving" | "saved" | null => {
    if (draft !== null && !sameBonus(draft, serverBonus)) return "saving";
    if (teamId in bonuses) return "saved";
    return null;
  };

  const change = (next: TeamBonus): void => {
    if (!selfTracking) return;
    setDraft(next);
    schedule(teamId, () => submitBonusPoints(room.name, teamId, next, setErrorMessage));
  };

  return (
    <div className="p-4">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {state.round} · {state.cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Bonus points</h2>

      <BonusCard
        playerName={currentPlayer.displayName}
        bonus={current}
        eligible={eligible}
        ineligibleReason={reason}
        editable={selfTracking}
        status={status()}
        onChange={change}
      />
    </div>
  );
}
