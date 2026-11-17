import { useRoundData } from "../../../../../context/RoundDataContext";
import { AdminCounterCard } from "../shared/AdminCounterCard";

export default function AdminView() {
  const { teams, bids, cardCount, canEdit, bidStatus, setBid, startingTeamId } = useRoundData();

  return (
    <div>
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
    </div>
  );
}
