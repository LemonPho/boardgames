import { useState } from "react";
import { useRoomContext } from "../../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../../context/AlertsContext";
import { submitTrickResult } from "../../../../../../api/skullKing";
import { useDebouncedSave } from "../../../../../../util/useDebouncedSave";
import { PlayerCounterCard } from "../../shared/PlayerCounterCard";

export default function PlayerView() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule } = useDebouncedSave();

  const [draft, setDraft] = useState<number | null>(null);

  if (!room || !currentPlayer || !currentPlayer.team || !state) return null;

  const teamId = currentPlayer.team.id;
  const trickResults = state.trickResults ?? {};
  const serverTricks = teamId in trickResults ? trickResults[teamId] : null;
  const selfTracking = room.trackingMode === "SELF";

  const value = draft ?? serverTricks ?? 0;

  const status = (): "saving" | "saved" | null => {
    if (draft !== null && draft !== serverTricks) return "saving";
    if (serverTricks !== null) return "saved";
    return null;
  };

  const change = (next: number): void => {
    if (!selfTracking || next < 0 || next > state.cardCount) return;
    setDraft(next);
    schedule(teamId, () => submitTrickResult(room.name, teamId, next, setErrorMessage));
  };

  return (
    <div className="p-4">
      <PlayerCounterCard
        title="Tricks won"
        value={value}
        round={state.round}
        cardCount={state.cardCount}
        status={status()}
        onIncrement={selfTracking ? () => change(value + 1) : undefined}
        onDecrement={selfTracking ? () => change(value - 1) : undefined}
      />
    </div>
  );
}
