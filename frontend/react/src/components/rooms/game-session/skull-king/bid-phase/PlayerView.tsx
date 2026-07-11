import { useState } from "react";
import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../context/AlertsContext";
import { submitBid } from "../../../../../api/skullKing";
import { PlayerCounterCard } from "../shared/PlayerCounterCard";

export default function PlayerView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  const [draft, setDraft] = useState<number>(0);

  if (!room || !currentPlayer || !currentPlayer.team || !state) return null;

  const bids = state.bids ?? {};
  const myBid = currentPlayer.team.id in bids ? bids[currentPlayer.team.id] : null;
  const submitted = myBid !== null;
  const selfTracking = room.trackingMode === "SELF";

  const handleIncrement = (): void => {
    if (draft >= state.cardCount) return;
    setDraft(prev => prev + 1);
  }

  const handleDecrement = (): void => {
    if (draft <= 0) return;
    setDraft(prev => prev - 1);
  }

  const handleSubmit = async (): Promise<void> => {
    if (!selfTracking || !currentPlayer.team) return;
    await submitBid(room.name, currentPlayer.team.id, draft, setErrorMessage);
  }

  return (
    <div className="p-4">
      <PlayerCounterCard
        title="Bids"
        value={submitted ? myBid : draft}
        round={state.round}
        cardCount={state.cardCount}
        submitted={submitted}
        submittedLabel="Bid locked in"
        submitLabel="Confirm bid"
        onIncrement={selfTracking && !submitted ? handleIncrement : undefined}
        onDecrement={selfTracking && !submitted ? handleDecrement : undefined}
        onSubmit={selfTracking && !submitted ? handleSubmit : undefined}
      />
    </div>
  );
}
