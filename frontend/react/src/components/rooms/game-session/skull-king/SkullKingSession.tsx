import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Trophy, XCircle } from "lucide-react";
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
import AdminActionBar from "./AdminActionBar";
import SessionAdminMenu, { type AdminMenuItem } from "./SessionAdminMenu";

const SCOREBOARD_PANEL = "skull-king-scoreboard";
const ADMIN_MENU_PANEL = "skull-king-admin-menu";

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

  const handleCancelGame = async (): Promise<void> => {
    if (!room) return;
    if (!window.confirm("Cancel this game? This ends the session for everyone and can't be undone.")) return;

    await cancelRoom(room.name, setErrorMessage);
    setSuccessMessage("Game cancelled");
    navigate("/");
  };

  // Admin menu items are added here (later: "Player view", "Reset round").
  // Destructive ones render last, below a divider.
  const adminMenuItems: AdminMenuItem[] = [
    {
      key: "cancel-game",
      label: "Cancel game",
      icon: <XCircle size={16} />,
      destructive: true,
      onSelect: handleCancelGame,
    },
  ];

  const renderPhase = () => {
    switch (state.gameState) {
      case "BIDS": return <BidPhase />;
      case "IN_PROGRESS": return <RoundInProgressPhase />;
      case "TRICK_RESULTS": return <TricksPhase />;
      case "BONUS_POINTS": return <BonusPointsPhase />;
      default: return <div className="p-4 text-center text-sm text-neutral-500">Unknown state</div>;
    }
  };

  // w-full: the layout's <main> centers its children (items-center), so without an
  // explicit width this column shrinks to its content — which collapses phases with
  // little content (e.g. the in-progress bid card) on narrow screens.
  return (
    <div className="w-full">
      {/* Icon-only actions keep the header to one row at 360 px, leaving room for
          the round info (and for future admin controls). */}
      <header className="flex items-center justify-between gap-2 px-4 h-12 border-b border-neutral-200 dark:border-neutral-800">
        <span className="min-w-0 text-sm text-neutral-500 dark:text-neutral-400 truncate">
          Round {state.round} · {state.cardCount} cards
        </span>
        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            aria-label="Scoreboard"
            onClick={(event) => { event.stopPropagation(); togglePanel(SCOREBOARD_PANEL); }}
            className="h-9 w-9 flex items-center justify-center rounded-lg border border-neutral-200 dark:border-neutral-700 hover:border-neutral-400 dark:hover:border-neutral-500 transition"
          >
            <Trophy size={18} />
          </button>
          {isAdmin && <SessionAdminMenu id={ADMIN_MENU_PANEL} items={adminMenuItems} />}
        </div>
      </header>

      {/* Key on the round so drafts and card state fully reset when a new round starts.
          The action bar sits inside so it reads the same RoundData as the phase. */}
      <LiveRoundDataProvider key={state.round}>
        {renderPhase()}
        <AdminActionBar />
      </LiveRoundDataProvider>

      <ScoreboardModal id={SCOREBOARD_PANEL} onOpenRound={setViewingRound} />
    </div>
  );
}
