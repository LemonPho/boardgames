import { useState } from "react";
import { bonusEligibility, expectedTrickTotal } from "../types/skull-king";
import { useSkullKingSessionContext } from "./SkullKingSessionContext";
import { useRoomContext } from "./RoomContext";
import { useAlertsContext } from "./AlertsContext";
import { RoundDataProvider, type RoundData } from "./RoundDataContext";
import { correctBids, correctTricks, setKraken } from "../api/skullKing";

// The earlier phases of the current round the admin can reopen. IN_PROGRESS holds no
// data, and the bonus phase is never "earlier" (it's the last phase of a round).
export type ReviewPhase = "BIDS" | "TRICK_RESULTS";

/**
 * Feeds RoundData for the admin's review of an earlier phase of the *current* round, so
 * the phase's existing AdminView renders unchanged. Unlike the live provider, edits stay
 * local drafts until Save, which sends the whole section through the batch correction
 * endpoint (like PastRoundView): a half-edited tricks section never reaches the server,
 * and the server's trick-total rule is checked once, on the complete set.
 *
 * The live phase doesn't move while reviewing. Corrections only rescore finished rounds,
 * so fixing the current round never scores it early.
 */
export function CurrentRoundReviewProvider({
  phase,
  onDone,
  children,
}: {
  phase: ReviewPhase;
  // Leaves review (after a successful Save); the caller swaps back to the live phase.
  onDone: () => void;
  children: React.ReactNode;
}) {
  const { state } = useSkullKingSessionContext();
  const { room } = useRoomContext();
  const { setErrorMessage } = useAlertsContext();

  // Drafts sit on top of the live state rather than copying it at open time, so values
  // the admin hasn't touched keep following the server while the review is open.
  const [bidDrafts, setBidDrafts] = useState<Map<string, number>>(new Map());
  const [trickDrafts, setTrickDrafts] = useState<Map<string, number>>(new Map());
  // null = no local edit, use the server's flag.
  const [krakenDraft, setKrakenDraft] = useState<boolean | null>(null);

  if (!state || !room) return null;

  const effective = (server: Record<string, number>, drafts: Map<string, number>): Record<string, number> => {
    const merged: Record<string, number> = { ...server };
    drafts.forEach((value, teamId) => { merged[teamId] = value; });
    return merged;
  };

  const bids = effective(state.bids ?? {}, bidDrafts);
  const trickResults = effective(state.trickResults ?? {}, trickDrafts);
  const serverKraken = state.krakenPlayed ?? false;
  const krakenPlayed = krakenDraft ?? serverKraken;

  const setBid = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    setBidDrafts(prev => new Map(prev).set(teamId, value));
  };

  const setTricks = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    setTrickDrafts(prev => new Map(prev).set(teamId, value));
  };

  // Every team is sent: the correction endpoints replace the whole section. Teams with no
  // value count as 0, as the server's prefill did when the phase was advanced.
  const teamValues = (values: Record<string, number>) =>
    state.teams.map((t) => ({ teamId: t.id, value: values[t.id] ?? 0 }));

  // A failed correction rejects here (the API layer shows the toast), so review stays
  // open with the drafts intact. The WebSocket push brings everyone the corrected values.
  const saveBids = async (): Promise<void> => {
    await correctBids(room.name, state.round, teamValues(bids), setErrorMessage);
    onDone();
  };

  const saveTricks = async (): Promise<void> => {
    // Set the Kraken flag first so the trick-total validation uses the new expected sum.
    if (krakenPlayed !== serverKraken) {
      await setKraken(room.name, state.round, krakenPlayed, setErrorMessage);
    }
    await correctTricks(room.name, state.round, teamValues(trickResults), setErrorMessage);
    onDone();
  };

  // Same tricks gate as the live provider (and the server's correctTricks check): while
  // the totals don't add up, the disabled Save button shows the count instead.
  const tricksBlockedLabel = (): string | null => {
    if (phase !== "TRICK_RESULTS") return null;
    const expected = expectedTrickTotal(state.cardCount, krakenPlayed);
    const total = state.teams.reduce((sum, t) => sum + (trickResults[t.id] ?? 0), 0);
    if (total === expected) return null;
    return `Tricks ${total} / ${expected}${krakenPlayed ? " (Kraken)" : ""}`;
  };

  const blockedLabel = tricksBlockedLabel();

  const value: RoundData = {
    round: state.round,
    cardCount: state.cardCount,
    // The phase being reviewed, not the live one: this data describes that phase.
    gameState: phase,
    teams: state.teams,
    startingTeamId: state.startingTeamId,
    advancedCards: room.configuration?.advancedCards ?? false,
    bids,
    trickResults,
    bonuses: state.bonuses ?? {},
    // Review is admin-only, and the admin may correct every team.
    canEdit: () => true,
    bonusEligibilityFor: (teamId) => bonusEligibility(bids[teamId], trickResults[teamId]),
    // No autosave, so there's no per-team save status to show.
    bidStatus: () => null,
    trickStatus: () => null,
    bonusStatus: () => null,
    setBid,
    setTricks,
    // Bonuses are only ever edited in the live bonus phase, never reviewed.
    setBonus: () => {},
    krakenPlayed,
    setKrakenPlayed: setKrakenDraft,
    advance: phase === "BIDS" ? saveBids : saveTricks,
    advanceLabel: blockedLabel ?? (phase === "BIDS" ? "Save bids" : "Save tricks"),
    advanceDisabled: blockedLabel !== null,
    showAdminView: true,
  };

  return <RoundDataProvider value={value}>{children}</RoundDataProvider>;
}
