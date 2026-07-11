import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitTrickResult } from "../../../../../../api/skullKing";
import { PlayerCounterCard } from "../../shared/PlayerCounterCard";

export default function PlayerView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  const [draft, setDraft] = useState<number>(0);

  if (!room || !currentPlayer || !currentPlayer.team || !state) return null;

  const trickResults = state.trickResults ?? {};
  const myTricksWon = currentPlayer.team.id in trickResults ? trickResults[currentPlayer.team.id] : null;
  const submitted = myTricksWon !== null;
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
    await submitTrickResult(room.name, currentPlayer.team.id, draft, setErrorMessage);
  }

  return (
    <div className="p-4">
      <PlayerCounterCard
        title="Tricks won"
        value={submitted ? myTricksWon : draft}
        round={state.round}
        cardCount={state.cardCount}
        submitted={submitted}
        submittedLabel="Tricks locked in"
        submitLabel="Confirm tricks"
        onIncrement={selfTracking && !submitted ? handleIncrement : undefined}
        onDecrement={selfTracking && !submitted ? handleDecrement : undefined}
        onSubmit={selfTracking && !submitted ? handleSubmit : undefined}
      />
    </div>
  );
}
