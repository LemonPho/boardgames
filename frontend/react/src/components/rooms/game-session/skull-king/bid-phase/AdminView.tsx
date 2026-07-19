import { useState } from "react";
import { useRoundData } from "../../../../../context/RoundDataContext";
import { AdminCounterCard } from "../shared/AdminCounterCard";
import SubmitButton from "../../../../util/SubmitButton";

export default function AdminView() {
  const { teams, bids, cardCount, canEdit, bidStatus, setBid, advance, advanceLabel, startingTeamId } = useRoundData();
  const [advancing, setAdvancing] = useState(false);

  return (
    <div className="p-4">
      <h2 className="text-lg font-medium text-center mb-6">Bids</h2>
      <div className="grid grid-cols-2 gap-3 mb-4">
        {teams.map((team) => (
          <AdminCounterCard
            key={team.id}
            playerName={team.player.displayName}
            editable={canEdit(team.id)}
            team={team}
            serverValue={bids[team.id] ?? 0}
            max={cardCount}
            status={bidStatus(team.id)}
            leads={team.id === startingTeamId}
            change={setBid}
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
