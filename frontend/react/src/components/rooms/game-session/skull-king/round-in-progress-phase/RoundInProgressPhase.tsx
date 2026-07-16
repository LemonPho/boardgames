import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { useAlertsContext } from "../../../../../context/AlertsContext";
import { startTrickResults } from "../../../../../api/skullKing";
import { MyBidCard } from "../shared/MyBidCard";

export default function RoundInProgressPhase() {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();
  const { setErrorMessage } = useAlertsContext();

  if (!room || !currentPlayer || !state) return null;

  const isAdmin = currentPlayer.role === "ADMIN";
  const bids = state.bids ?? {};
  const myBid = currentPlayer.team ? bids[currentPlayer.team.id] ?? null : null;

  const handleStartTrickResults = async (): Promise<void> => {
    await startTrickResults(room.name, setErrorMessage);
  }

  return (
    <div className="p-4 max-w-md mx-auto">
      <div className="mb-4">
        <MyBidCard
          round={state.round}
          cardCount={state.cardCount}
          bid={myBid}
          leads={currentPlayer.team ? currentPlayer.team.id === state.startingTeamId : false}
        />
      </div>

      {isAdmin && (
        <button
          type="button"
          onClick={handleStartTrickResults}
          className="w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 dark:bg-neutral-100 text-white dark:text-neutral-900 disabled:opacity-40 transition active:scale-[0.98]"
        >
          Enter trick results
        </button>
      )}
    </div>
  );
}
