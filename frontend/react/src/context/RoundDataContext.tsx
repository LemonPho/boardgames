import { createContext, useContext, useState } from "react";
import type { TeamResponse } from "../types/teams";
import type { BonusEligibility, SkullKingGameState, TeamBonus } from "../types/skull-king";
import { EMPTY_BONUS, bonusEligibility, expectedTrickTotal } from "../types/skull-king";
import { useSkullKingSessionContext } from "./SkullKingSessionContext";
import { useRoomContext } from "./RoomContext";
import { useAlertsContext } from "./AlertsContext";
import { useDebouncedSave } from "../util/useDebouncedSave";
import { canEditTeam } from "../components/rooms/game-session/skull-king/shared/permissions";
import {
  submitBid,
  submitTrickResult,
  submitBonusPoints,
  startRound,
  finishRound,
  startBonusPoints,
  startTrickResults,
  setKraken,
} from "../api/skullKing";

export type SaveStatus = "saving" | "saved" | null;

/**
 * The per-round data + edit operations the phase sections depend on. Both the live
 * game and the read-only past-round view provide this, so the same section
 * components (bid/trick/bonus card grids) render either source.
 */
export interface RoundData {
  round: number;
  cardCount: number;
  gameState: SkullKingGameState;
  teams: TeamResponse[];
  // The team that leads (goes first) this round.
  startingTeamId: string;
  // Whether the room includes advanced cards (Loot, Kraken, White Whale).
  advancedCards: boolean;

  // Effective values (a pending local edit takes precedence over the server value).
  bids: Record<string, number>;
  trickResults: Record<string, number>;
  bonuses: Record<string, TeamBonus>;

  canEdit: (teamId: string) => boolean;
  bonusEligibilityFor: (teamId: string) => BonusEligibility;

  bidStatus: (teamId: string) => SaveStatus;
  trickStatus: (teamId: string) => SaveStatus;
  bonusStatus: (teamId: string) => SaveStatus;

  setBid: (teamId: string, value: number) => void;
  setTricks: (teamId: string, value: number) => void;
  setBonus: (teamId: string, bonus: TeamBonus) => void;

  // A Kraken destroyed a trick this round: teams' tricks must sum to cardCount − 1.
  krakenPlayed: boolean;
  setKrakenPlayed: (value: boolean) => void;

  // The admin action that advances past the current phase; null when there's
  // nothing to advance (e.g. a read-only past round).
  advance: (() => Promise<void>) | null;
  // Button text. While advance is blocked it carries the reason instead (e.g. the
  // tricks phase shows "Tricks 3 / 5" in the disabled button rather than separate text).
  advanceLabel: string | null;
  // Admin advance is blocked (e.g. tricks don't add up). The button should render disabled.
  advanceDisabled: boolean;

  // Render the admin-style (all teams) phase UI. False for players, and for an admin who
  // switched to Player view in a SELF room.
  showAdminView: boolean;
}

const RoundDataContext = createContext<RoundData | null>(null);

const sameBonus = (a: TeamBonus | undefined, b: TeamBonus | undefined): boolean =>
  JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

const MAX_ROUNDS = 10;

/**
 * Feeds RoundData from the live session: reshapes SkullKingSessionContext.state,
 * owns the drafts + debounced saves, and wires the phase-advance action.
 */
export function LiveRoundDataProvider({
  children,
  viewAsPlayer = false,
}: {
  children: React.ReactNode;
  // Admin's Player view (SELF rooms): only changes what's rendered; editing rights,
  // the advance action and the received state stay the admin's.
  viewAsPlayer?: boolean;
}) {
  const { state, refreshState } = useSkullKingSessionContext();
  const { room, currentPlayer } = useRoomContext();
  const { setErrorMessage, clearAlerts } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [bidDrafts, setBidDrafts] = useState<Map<string, number>>(new Map());
  const [trickDrafts, setTrickDrafts] = useState<Map<string, number>>(new Map());
  const [bonusDrafts, setBonusDrafts] = useState<Map<string, TeamBonus>>(new Map());
  // Optimistic Kraken value: the toggle reflects the tap immediately instead of
  // waiting for debounce + POST + WebSocket push. null = no local edit, use server.
  const [krakenDraft, setKrakenDraft] = useState<boolean | null>(null);

  if (!state || !room || !currentPlayer) return null;

  const serverBids = state.bids ?? {};
  const serverTricks = state.trickResults ?? {};
  const serverBonuses = state.bonuses ?? {};

  const effective = <T,>(server: Record<string, T>, drafts: Map<string, T>): Record<string, T> => {
    const merged: Record<string, T> = { ...server };
    drafts.forEach((value, teamId) => { merged[teamId] = value; });
    return merged;
  };

  const bids = effective(serverBids, bidDrafts);
  const trickResults = effective(serverTricks, trickDrafts);
  const bonuses = effective(serverBonuses, bonusDrafts);

  const canEdit = (teamId: string): boolean => canEditTeam(room, currentPlayer, teamId);

  const bonusEligibilityFor = (teamId: string) => bonusEligibility(bids[teamId], trickResults[teamId]);

  const numericStatus = (
    teamId: string,
    server: Record<string, number>,
    drafts: Map<string, number>,
  ): SaveStatus => {
    if (drafts.has(teamId) && drafts.get(teamId) !== server[teamId]) return "saving";
    if (teamId in server) return "saved";
    return null;
  };

  const bidStatus = (teamId: string) => numericStatus(teamId, serverBids, bidDrafts);
  const trickStatus = (teamId: string) => numericStatus(teamId, serverTricks, trickDrafts);
  const bonusStatus = (teamId: string): SaveStatus => {
    if (bonusDrafts.has(teamId) && !sameBonus(bonusDrafts.get(teamId), serverBonuses[teamId])) return "saving";
    if (teamId in serverBonuses) return "saved";
    return null;
  };

  const setBid = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    setBidDrafts(prev => new Map(prev).set(teamId, value));
    schedule(teamId, () => submitBid(room.name, teamId, value, setErrorMessage));
  };

  const setTricks = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    setTrickDrafts(prev => new Map(prev).set(teamId, value));
    schedule(teamId, () => submitTrickResult(room.name, teamId, value, setErrorMessage));
  };

  const setBonus = (teamId: string, value: TeamBonus): void => {
    // A zero bid can only earn Loot, and the backend rejects any other field. Stray
    // non-loot values can linger (e.g. a correction changed the bid from 2 to 0), so
    // strip them before saving.
    const bonus = bonusEligibilityFor(teamId).lootOnly ? { ...EMPTY_BONUS, loot: value.loot } : value;
    setBonusDrafts(prev => new Map(prev).set(teamId, bonus));
    schedule(teamId, () => submitBonusPoints(room.name, teamId, bonus, setErrorMessage));
  };

  const setKrakenPlayed = (value: boolean): void => {
    setKrakenDraft(value);
    schedule("__kraken__", async () => {
      try {
        await setKraken(room.name, state.round, value, setErrorMessage);
      } catch (error) {
        // Save failed: drop the optimistic value so the toggle shows the server's.
        setKrakenDraft(null);
        throw error;
      }
    });
  };

  // --- Phase advance (admin only; each phase submits any stragglers, flushes, then transitions) ---

  // The phase transition can fail on a flaky connection even though the server
  // already advanced (its response or the WebSocket push was lost) — a retry then
  // errors with "wrong phase". So on failure we re-sync state: if the server moved
  // past the phase we started from, the advance already worked, so we swallow the
  // error and let the fresh state re-render the correct phase. Otherwise (a real
  // validation error, e.g. not all bids in) we surface it.
  const runTransition = async (fromPhase: SkullKingGameState, transition: () => Promise<void>): Promise<void> => {
    try {
      await transition();
    } catch (error) {
      const fresh = await refreshState();
      if (fresh && fresh.gameState !== fromPhase) {
        clearAlerts();
        return;
      }
      throw error;
    }
  };

  // Teams without a bid/trick value are defaulted to 0 by the server, inside the
  // transition's own transaction — so advancing is a flush plus one request, with
  // no burst of client-side 0 submits racing the transition.
  const advanceBids = async (): Promise<void> => {
    await flush();
    await runTransition("BIDS", () => startRound(room.name, setErrorMessage));
  };

  // Nothing is edited while the round is being played, so there's nothing to flush.
  const advanceInProgress = (): Promise<void> =>
    runTransition("IN_PROGRESS", () => startTrickResults(room.name, setErrorMessage));

  const advanceTricks = async (): Promise<void> => {
    await flush();
    await runTransition("TRICK_RESULTS", () => startBonusPoints(room.name, setErrorMessage));
  };

  const advanceBonus = async (): Promise<void> => {
    // Teams with a draft (including a lootOnly team's Loot, saved via setBonus) aren't
    // pending; flush() below lands their save. A lootOnly team counts as eligible, so
    // even if it were pending its effective value would be submitted, not EMPTY_BONUS.
    const pending = state.teams.filter((t) => !(t.id in serverBonuses) && !bonusDrafts.has(t.id));
    for (const t of pending) {
      const eligible = bonusEligibility(bids[t.id], trickResults[t.id]).eligible;
      await submitBonusPoints(room.name, t.id, eligible ? (bonuses[t.id] ?? EMPTY_BONUS) : EMPTY_BONUS, setErrorMessage);
    }
    await flush();
    await runTransition("BONUS_POINTS", () => finishRound(room.name, setErrorMessage));
  };

  const isAdmin = currentPlayer.role === "ADMIN";

  const advanceFor = (): { advance: (() => Promise<void>) | null; label: string | null } => {
    if (!isAdmin) return { advance: null, label: null };
    switch (state.gameState) {
      case "BIDS": return { advance: advanceBids, label: "Start round" };
      case "IN_PROGRESS": return { advance: advanceInProgress, label: "Enter trick results" };
      case "TRICK_RESULTS": return { advance: advanceTricks, label: "Continue to bonus points" };
      case "BONUS_POINTS":
        return { advance: advanceBonus, label: state.round >= MAX_ROUNDS ? "Finish game" : "Finish round" };
      default: return { advance: null, label: null };
    }
  };

  const { advance, label: baseAdvanceLabel } = advanceFor();

  const krakenPlayed = krakenDraft ?? state.krakenPlayed ?? false;

  // Tricks gate: mirrors the server's startBonusPoints check (via expectedTrickTotal) so
  // the admin sees the problem before a round trip. It reads the effective values, so it
  // reacts on every tap rather than after the debounced save. Teams with no value count
  // as 0, matching the server's prefill on transition. The server stays the authority.
  // While blocked, the disabled button itself shows the count (e.g. "Tricks 3 / 5").
  // Other phases never block: missing bids/bonuses are defaulted by the server.
  const advanceGate = (): { disabled: boolean; blockedLabel: string | null } => {
    if (state.gameState !== "TRICK_RESULTS") return { disabled: false, blockedLabel: null };
    const expected = expectedTrickTotal(state.cardCount, krakenPlayed);
    const total = state.teams.reduce((sum, t) => sum + (trickResults[t.id] ?? 0), 0);
    if (total === expected) return { disabled: false, blockedLabel: null };
    return { disabled: true, blockedLabel: `Tricks ${total} / ${expected}${krakenPlayed ? " (Kraken)" : ""}` };
  };

  const { disabled: advanceDisabled, blockedLabel } = advanceGate();
  const advanceLabel = blockedLabel ?? baseAdvanceLabel;

  const value: RoundData = {
    round: state.round,
    cardCount: state.cardCount,
    gameState: state.gameState,
    teams: state.teams,
    startingTeamId: state.startingTeamId,
    advancedCards: room.configuration?.advancedCards ?? false,
    bids,
    trickResults,
    bonuses,
    canEdit,
    bonusEligibilityFor,
    bidStatus,
    trickStatus,
    bonusStatus,
    setBid,
    setTricks,
    setBonus,
    krakenPlayed,
    setKrakenPlayed,
    advance,
    advanceLabel,
    advanceDisabled,
    showAdminView: isAdmin && !viewAsPlayer,
  };

  return <RoundDataContext.Provider value={value}>{children}</RoundDataContext.Provider>;
}

export function useRoundData(): RoundData {
  const ctx = useContext(RoundDataContext);
  if (!ctx) throw new Error("useRoundData must be used within a RoundData provider");
  return ctx;
}
