import { useState } from "react";
import { useRoundData } from "../../../../../../context/RoundDataContext";
import { AdminCounterCard } from "../../shared/AdminCounterCard";
import { KrakenToggle } from "../../shared/KrakenToggle";
import SubmitButton from "../../../../../util/SubmitButton";

export default function AdminView() {
  const { teams, trickResults, cardCount, canEdit, trickStatus, setTricks, advance, advanceLabel, krakenPlayed, setKrakenPlayed, startingTeamId, advancedCards } = useRoundData();
  const [advancing, setAdvancing] = useState(false);

  // The Kraken is a round-level fact registered by the admin; `advance` is the
  // admin-only signal for the live round.
  const isAdmin = advance !== null;

  return (
    <div className="p-4">
      <h2 className="text-lg font-medium text-center mb-6">Tricks won</h2>

      {advancedCards && (
        <div className="mb-4">
          <KrakenToggle krakenPlayed={krakenPlayed} editable={isAdmin} onToggle={setKrakenPlayed} />
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 mb-4">
        {teams.map((team) => (
          <AdminCounterCard
            key={team.id}
            playerName={team.player.displayName}
            editable={canEdit(team.id)}
            team={team}
            serverValue={trickResults[team.id] ?? 0}
            max={cardCount}
            status={trickStatus(team.id)}
            leads={team.id === startingTeamId}
            change={setTricks}
          />
        ))}
      </div>

      {advance && (
        <SubmitButton
          text={advanceLabel ?? "Continue"}
          loading={advancing}
          setLoading={setAdvancing}
          onSubmit={advance}
          className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
        />
      )}
    </div>
  );
}
