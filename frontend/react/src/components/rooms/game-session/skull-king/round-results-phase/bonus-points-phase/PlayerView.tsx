import { useRoomContext } from "../../../../../../context/RoomContext";
import { useRoundData } from "../../../../../../context/RoundDataContext";
import { EMPTY_BONUS } from "../../../../../../types/skull-king";
import { BonusCard } from "../../shared/BonusCard";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { round, cardCount, bonuses, canEdit, bonusEligibilityFor, bonusStatus, setBonus } = useRoundData();

  if (!currentPlayer || !currentPlayer.team) return null;

  const teamId = currentPlayer.team.id;
  const { eligible, reason } = bonusEligibilityFor(teamId);

  return (
    <div className="p-4">
      <div className="text-center mb-1">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {round} · {cardCount} cards
        </span>
      </div>
      <h2 className="text-lg font-medium text-center mb-6">Bonus points</h2>

      <BonusCard
        playerName={currentPlayer.displayName}
        bonus={bonuses[teamId] ?? EMPTY_BONUS}
        eligible={eligible}
        ineligibleReason={reason}
        editable={canEdit(teamId)}
        status={bonusStatus(teamId)}
        onChange={(next) => setBonus(teamId, next)}
      />
    </div>
  );
}
