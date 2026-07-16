import { useRoundData } from "../../../../../../context/RoundDataContext";
import { EMPTY_BONUS } from "../../../../../../types/skull-king";
import { BonusCard } from "../../shared/BonusCard";

export default function AdminView() {
  const { teams, bonuses, canEdit, bonusEligibilityFor, bonusStatus, setBonus, advance, advanceLabel, advancedCards } = useRoundData();

  return (
    <div className="p-4">
      <h2 className="text-lg font-medium text-center mb-6">Bonus points</h2>

      <div className="flex flex-col gap-3 mb-4">
        {teams.map((team) => {
          const { eligible, reason } = bonusEligibilityFor(team.id);
          return (
            <BonusCard
              key={team.id}
              playerName={team.player.displayName}
              bonus={bonuses[team.id] ?? EMPTY_BONUS}
              eligible={eligible}
              ineligibleReason={reason}
              editable={canEdit(team.id)}
              advancedCards={advancedCards}
              status={bonusStatus(team.id)}
              onChange={(next) => setBonus(team.id, next)}
            />
          );
        })}
      </div>

      {advance && (
        <button
          type="button"
          onClick={advance}
          className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
        >
          {advanceLabel}
        </button>
      )}
    </div>
  );
}
