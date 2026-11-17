import { useRoundData } from "../../../../../context/RoundDataContext";
import { EMPTY_BONUS } from "../../../../../types/skull-king";
import { AdminCounterCard } from "./AdminCounterCard";
import { BonusCard } from "./BonusCard";

/**
 * Shown under the admin's own card in Player view. Anonymous players can't log in, so
 * in a SELF room the admin is the only one who can enter their values; hiding them in
 * Player view would leave their bids/tricks/bonuses impossible to enter.
 */
export default function AnonymousTeamsSection({ phase }: { phase: "bids" | "tricks" | "bonus" }) {
  const {
    teams, cardCount, startingTeamId, advancedCards, canEdit,
    bids, bidStatus, setBid,
    trickResults, trickStatus, setTricks,
    bonuses, bonusStatus, setBonus, bonusEligibilityFor,
  } = useRoundData();

  const anonymousTeams = teams.filter((t) => t.player.role === "ANONYMOUS");
  if (anonymousTeams.length === 0) return null;

  return (
    <div>
      <h3 className="text-sm font-medium text-neutral-500 mt-6 mb-3">Anonymous players</h3>

      {phase === "bonus" ? (
        <div className="flex flex-col gap-3 mb-4">
          {anonymousTeams.map((team) => {
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
      ) : (
        <div className="grid grid-cols-2 gap-3 mb-4">
          {anonymousTeams.map((team) => (
            <AdminCounterCard
              key={team.id}
              playerName={team.player.displayName}
              editable={canEdit(team.id)}
              team={team}
              serverValue={(phase === "bids" ? bids : trickResults)[team.id] ?? 0}
              max={cardCount}
              status={phase === "bids" ? bidStatus(team.id) : trickStatus(team.id)}
              leads={team.id === startingTeamId}
              change={phase === "bids" ? setBid : setTricks}
            />
          ))}
        </div>
      )}
    </div>
  );
}
