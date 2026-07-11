import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitBonusPoints } from "../../../../../../api/skullKing";
import { EMPTY_BONUS, bonusEligibility, type TeamBonus } from "../../../../../../types/skull-king";
import { BonusCard } from "../../shared/BonusCard";

export default function PlayerView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  const [draft, setDraft] = useState<TeamBonus | null>(null);

  if (!room || !currentPlayer || !currentPlayer.team || !state) return null;

  const teamId = currentPlayer.team.id;
  const bids = state.bids ?? {};
  const trickResults = state.trickResults ?? {};
  const bonuses = state.bonuses ?? {};

  const { eligible, reason } = bonusEligibility(bids[teamId], trickResults[teamId]);
  const submitted = teamId in bonuses;
  const selfTracking = room.trackingMode === "SELF";

  const current = draft ?? bonuses[teamId] ?? EMPTY_BONUS;

  const handleSubmit = async (): Promise<void> => {
    if (!selfTracking || !currentPlayer.team) return;
    const bonus = eligible ? current : EMPTY_BONUS;
    await submitBonusPoints(room.name, currentPlayer.team.id, bonus, setErrorMessage);
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
        submitted={submitted}
        editable={selfTracking}
        onChange={setDraft}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
