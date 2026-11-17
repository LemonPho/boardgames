import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Hash, ListChecks, RotateCcw, Trophy, User, Users, XCircle } from "lucide-react";
import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";
import { LiveRoundDataProvider } from "../../../../context/RoundDataContext";
import { CurrentRoundReviewProvider, type ReviewPhase } from "../../../../context/CurrentRoundReviewProvider";
import { useUIContext } from "../../../../context/UIContext";
import { useRoomContext } from "../../../../context/RoomContext";
import { useAlertsContext } from "../../../../context/AlertsContext";
import { cancelRoom } from "../../../../api/rooms";
import { resetRound } from "../../../../api/skullKing";
import BidPhase from "./bid-phase/BidPhase";
import BidsAdminView from "./bid-phase/AdminView";
import TricksAdminView from "./round-results-phase/tricks-phase/AdminView";
import RoundInProgressPhase from "./round-in-progress-phase/RoundInProgressPhase";
import BonusPointsPhase from "./round-results-phase/bonus-points-phase/BonusPointsPhase";
import TricksPhase from "./round-results-phase/tricks-phase/TricksPhase";
import ScoreboardModal from "./ScoreboardModal";
import PastRoundView from "./PastRoundView";
import AdminActionBar from "./AdminActionBar";
import SessionAdminMenu, { type AdminMenuItem } from "./SessionAdminMenu";
import { usePlayerViewPreference } from "../../../../util/usePlayerViewPreference";
import type { SkullKingGameState } from "../../../../types/skull-king";

const SCOREBOARD_PANEL = "skull-king-scoreboard";
const ADMIN_MENU_PANEL = "skull-king-admin-menu";

const PHASE_LABELS: Record<SkullKingGameState, string> = {
  BIDS: "Bids",
  IN_PROGRESS: "Playing",
  TRICK_RESULTS: "Tricks",
  BONUS_POINTS: "Bonus",
};

export default function SkullKingSession() {
  const { state } = useSkullKingSessionContext();
  const { room, currentPlayer } = useRoomContext();
  const { togglePanel } = useUIContext();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const navigate = useNavigate();

  const [viewingRound, setViewingRound] = useState<number | null>(null);
  // Lives here, above the round-keyed provider, so it doesn't reset every round.
  const [playerViewPref, setPlayerViewPref] = usePlayerViewPreference(room?.name);
  // The admin's review of an earlier phase of the current round. It's tagged with the live
  // round + phase it was opened in, so it closes by itself once the live phase moves on
  // (round finished or reset, phase advanced from another device) rather than via an
  // effect resetting it.
  const [review, setReview] = useState<{ phase: ReviewPhase; liveKey: string } | null>(null);

  if (!state) return null;

  const liveKey = `${state.roundKey ?? state.round}:${state.gameState}`;
  const reviewPhase = review?.liveKey === liveKey ? review.phase : null;
  const openReview = (phase: ReviewPhase): void => setReview({ phase, liveKey });
  const closeReview = (): void => setReview(null);

  const isAdmin = currentPlayer?.role === "ADMIN";
  // Only in SELF rooms: in ADMIN-tracked rooms the admin must enter everyone's values.
  // A stored preference is ignored when it isn't available.
  const playerViewAvailable = isAdmin && room?.trackingMode === "SELF";
  const viewAsPlayer = playerViewAvailable && playerViewPref;

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

  // The server always resets whichever round is current; finished rounds are corrected
  // in PastRoundView instead. The WebSocket push re-renders every client at BIDS with a
  // new roundKey, which remounts the round provider and drops all drafts.
  // Known edge case: a debounced save already in flight when the admin resets can land on
  // the new BIDS event (a stale bid is accepted; a stale trick/bonus save is rejected with a
  // wrong-phase toast). The remount clears pending timers, which is good enough here.
  const handleResetRound = async (): Promise<void> => {
    if (!room) return;
    if (!window.confirm(`Reset round ${state.round}? All bids, tricks and bonuses entered for this round will be discarded and the round restarts at bidding.`)) return;

    await resetRound(room.name, setErrorMessage);
    setSuccessMessage(`Round ${state.round} reset`);
  };

  // Admin menu items are added here. Destructive ones render last, below a divider.
  const adminMenuItems: AdminMenuItem[] = [
    {
      key: "player-view",
      label: viewAsPlayer ? "Show all players" : "Player view",
      icon: viewAsPlayer ? <Users size={16} /> : <User size={16} />,
      hidden: !playerViewAvailable,
      onSelect: () => setPlayerViewPref(!playerViewPref),
    },
    // Review an earlier phase of this round without moving the live phase. Only phases
    // already completed this round are offered (IN_PROGRESS holds no data).
    {
      key: "review-bids",
      label: "Review bids",
      icon: <ListChecks size={16} />,
      hidden: reviewPhase !== null || !(["IN_PROGRESS", "TRICK_RESULTS", "BONUS_POINTS"] as SkullKingGameState[]).includes(state.gameState),
      onSelect: () => openReview("BIDS"),
    },
    {
      key: "review-tricks",
      label: "Review tricks",
      icon: <Hash size={16} />,
      hidden: reviewPhase !== null || state.gameState !== "BONUS_POINTS",
      onSelect: () => openReview("TRICK_RESULTS"),
    },
    {
      key: "reset-round",
      label: "Reset round",
      icon: <RotateCcw size={16} />,
      destructive: true,
      onSelect: handleResetRound,
    },
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
        <div className="min-w-0 flex items-center gap-2">
          <span className="min-w-0 text-sm text-neutral-500 dark:text-neutral-400 truncate">
            Round {state.round} · {state.cardCount} cards
          </span>
          {/* Reminder that the admin is seeing the reduced, player-style screen. */}
          {viewAsPlayer && (
            <span className="shrink-0 px-2 py-0.5 rounded-full text-xs font-medium bg-neutral-100 text-neutral-600 dark:bg-neutral-800 dark:text-neutral-300">
              Player view
            </span>
          )}
        </div>
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

      {reviewPhase !== null ? (
        // Reviewing replaces the live provider and phase body, so the admin can't advance
        // the live phase from here: the action bar reads the review's RoundData and shows
        // Save. Leaving review remounts the live provider, which drops its drafts so the
        // corrected server values show through.
        <CurrentRoundReviewProvider phase={reviewPhase} onDone={closeReview}>
          <div className="p-4 max-w-md mx-auto">
            <div className="flex items-center justify-between gap-2 mb-4 px-3 py-2 rounded-lg border border-amber-200 bg-amber-50 text-sm">
              <span className="min-w-0 text-amber-900">
                Reviewing {PHASE_LABELS[reviewPhase]} · live phase is {PHASE_LABELS[state.gameState]}
              </span>
              <button
                type="button"
                onClick={closeReview}
                className="shrink-0 font-medium text-amber-900 underline underline-offset-2"
              >
                Back to live
              </button>
            </div>
            {reviewPhase === "BIDS" ? <BidsAdminView /> : <TricksAdminView />}
          </div>
          <AdminActionBar onCancel={closeReview} />
        </CurrentRoundReviewProvider>
      ) : (
        // Key on the round's identity (its BIDS event id, falling back to the number) so
        // drafts and card state fully reset when a new round starts AND when the round is
        // reset, which keeps the same round number.
        // The action bar sits inside so it reads the same RoundData as the phase.
        <LiveRoundDataProvider key={state.roundKey ?? state.round} viewAsPlayer={viewAsPlayer}>
          {renderPhase()}
          <AdminActionBar />
        </LiveRoundDataProvider>
      )}

      <ScoreboardModal id={SCOREBOARD_PANEL} onOpenRound={setViewingRound} />
    </div>
  );
}
