import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";
import { LiveRoundDataProvider } from "../../../../context/RoundDataContext";
import { useUIContext } from "../../../../context/UIContext";
import { useRoomContext } from "../../../../context/RoomContext";
import { useAlertsContext } from "../../../../context/AlertsContext";
import { cancelRoom } from "../../../../api/rooms";
import BidPhase from "./bid-phase/BidPhase";
import RoundInProgressPhase from "./round-in-progress-phase/RoundInProgressPhase";
import BonusPointsPhase from "./round-results-phase/bonus-points-phase/BonusPointsPhase";
import TricksPhase from "./round-results-phase/tricks-phase/TricksPhase";
import ScoreboardModal from "./ScoreboardModal";
import PastRoundView from "./PastRoundView";

const SCOREBOARD_PANEL = "skull-king-scoreboard";

export default function SkullKingSession() {
  const { state } = useSkullKingSessionContext();
  const { room, currentPlayer } = useRoomContext();
  const { togglePanel } = useUIContext();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const navigate = useNavigate();

  const [viewingRound, setViewingRound] = useState<number | null>(null);

  if (!state) return null;

  const isAdmin = currentPlayer?.role === "ADMIN";

  if (viewingRound !== null && room) {
    return (
      <PastRoundView
        roomName={room.name}
        round={viewingRound}
        onBack={() => setViewingRound(null)}
      />
    );
  }

  const handleCancelGame = async (event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();
    if (!room) return;
    if (!window.confirm("Cancel this game? This ends the session for everyone and can't be undone.")) return;

    await cancelRoom(room.name, setErrorMessage);
    setSuccessMessage("Game cancelled");
    navigate("/");
  };

  const renderPhase = () => {
    switch (state.gameState) {
      case "BIDS": return <BidPhase />;
      case "IN_PROGRESS": return <RoundInProgressPhase />;
      case "TRICK_RESULTS": return <TricksPhase />;
      case "BONUS_POINTS": return <BonusPointsPhase />;
      default: return <div className="p-4 text-center text-sm text-neutral-500">Unknown state</div>;
    }
  };

  return (
    <div>
      <header className="flex items-center justify-between px-4 h-12 border-b border-neutral-200 dark:border-neutral-800">
        <span className="text-sm text-neutral-500 dark:text-neutral-400">
          Round {state.round} · {state.cardCount} cards
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={(event) => { event.stopPropagation(); togglePanel(SCOREBOARD_PANEL); }}
            className="text-sm font-medium px-3 py-1.5 rounded-lg border border-neutral-200 dark:border-neutral-700 hover:border-neutral-400 dark:hover:border-neutral-500 transition"
          >
            Scoreboard
          </button>
          {isAdmin && (
            <button
              type="button"
              onClick={(event) => { handleCancelGame(event); }}
              className="text-sm font-medium px-3 py-1.5 rounded-lg border border-red-200 text-red-500 hover:border-red-400 transition"
            >
              Cancel game
            </button>
          )}
        </div>
      </header>

      {/* Key on the round so drafts and card state fully reset when a new round starts. */}
      <LiveRoundDataProvider key={state.round}>
        {renderPhase()}
      </LiveRoundDataProvider>

      <ScoreboardModal id={SCOREBOARD_PANEL} onOpenRound={setViewingRound} />
    </div>
  );
}
