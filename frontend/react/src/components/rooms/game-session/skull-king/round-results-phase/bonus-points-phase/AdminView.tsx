import { useRoundData } from "../../../../../../context/RoundDataContext";
import { EMPTY_BONUS } from "../../../../../../types/skull-king";
import { BonusCard } from "../../shared/BonusCard";

export default function AdminView() {
  const { teams, bonuses, canEdit, bonusEligibilityFor, bonusStatus, setBonus, advancedCards } = useRoundData();

  return (
    <div>
      <h2 className="text-lg font-medium text-center mb-6">Bonus points</h2>

      <div className="flex flex-col gap-3 mb-4">
        {teams.map((team) => {
          const { eligible, reason, lootOnly } = bonusEligibilityFor(team.id);
          return (
            <BonusCard
              key={team.id}
              playerName={team.player.displayName}
              bonus={bonuses[team.id] ?? EMPTY_BONUS}
              eligible={eligible}
              ineligibleReason={reason}
              lootOnly={lootOnly}
              editable={canEdit(team.id)}
              advancedCards={advancedCards}
              status={bonusStatus(team.id)}
              onChange={(next) => setBonus(team.id, next)}
            />
          );
        })}
      </div>
    </div>
  );
}
