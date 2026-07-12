import { useRoomContext } from "../../../../../../context/RoomContext";
import { useRoundData } from "../../../../../../context/RoundDataContext";
import { PlayerCounterCard } from "../../shared/PlayerCounterCard";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { round, cardCount, trickResults, canEdit, trickStatus, setTricks } = useRoundData();

  if (!currentPlayer || !currentPlayer.team) return null;

  const teamId = currentPlayer.team.id;
  const value = trickResults[teamId] ?? 0;
  const editable = canEdit(teamId);

  return (
    <div className="p-4">
      <PlayerCounterCard
        title="Tricks won"
        value={value}
        round={round}
        cardCount={cardCount}
        status={trickStatus(teamId)}
        onIncrement={editable ? () => setTricks(teamId, value + 1) : undefined}
        onDecrement={editable ? () => setTricks(teamId, value - 1) : undefined}
      />
    </div>
  );
}
