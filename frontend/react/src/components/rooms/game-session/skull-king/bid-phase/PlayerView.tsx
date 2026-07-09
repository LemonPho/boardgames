import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { BidsPlayerCard } from "../shared/BidsPlayerCard";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { bids, round, cardCount } = useSkullKingSessionContext();

  if (!currentPlayer || !currentPlayer.team) return null;

  const myBid = bids.get(currentPlayer.team.id);

  const tempFunction = (): void => {}

  return (
    <div className="p-4">
      <BidsPlayerCard
        round={round}
        cardCount={cardCount}
        bid={myBid?.bid ?? 0}
        submitted={!!myBid}
        onIncrement={tempFunction}
        onDecrement={tempFunction}
        onSubmit={tempFunction}
      />
    </div>
  );
}
