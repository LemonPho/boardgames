import { useRoomContext } from "../../../../../context/RoomContext";
import { useRoundData } from "../../../../../context/RoundDataContext";
import { PlayerCounterCard } from "../shared/PlayerCounterCard";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { round, cardCount, bids, canEdit, bidStatus, setBid, startingTeamId } = useRoundData();

  if (!currentPlayer || !currentPlayer.team) return null;

  const teamId = currentPlayer.team.id;
  const value = bids[teamId] ?? 0;
  const editable = canEdit(teamId);

  return (
    <div className="p-4">
      <PlayerCounterCard
        title="Bids"
        value={value}
        round={round}
        cardCount={cardCount}
        status={bidStatus(teamId)}
        leads={teamId === startingTeamId}
        onIncrement={editable ? () => setBid(teamId, value + 1) : undefined}
        onDecrement={editable ? () => setBid(teamId, value - 1) : undefined}
      />
    </div>
  );
}
