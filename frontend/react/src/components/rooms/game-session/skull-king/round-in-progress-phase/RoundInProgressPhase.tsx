import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { MyBidCard } from "../shared/MyBidCard";

// The admin's "Enter trick results" lives in the session's AdminActionBar
// (RoundData.advance), so this phase only shows the player's own bid.
export default function RoundInProgressPhase() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();

  if (!room || !currentPlayer || !state) return null;

  const bids = state.bids ?? {};
  const myBid = currentPlayer.team ? bids[currentPlayer.team.id] ?? null : null;

  return (
    <div className="p-4 max-w-md mx-auto">
      <MyBidCard
        round={state.round}
        cardCount={state.cardCount}
        bid={myBid}
        leads={currentPlayer.team ? currentPlayer.team.id === state.startingTeamId : false}
      />
    </div>
  );
}
