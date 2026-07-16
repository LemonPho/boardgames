import { createContext, useContext, useState } from "react";
import type { TeamResponse } from "../types/teams";
import type { BonusEligibility, SkullKingGameState, TeamBonus } from "../types/skull-king";
import { EMPTY_BONUS, bonusEligibility } from "../types/skull-king";
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
  startTrickResults,
  finishRound,
  startBonusPoints,
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
  advanceLabel: string | null;
}

const RoundDataContext = createContext<RoundData | null>(null);

const sameBonus = (a: TeamBonus | undefined, b: TeamBonus | undefined): boolean =>
  JSON.stringify(a ?? null) === JSON.stringify(b ?? null);

const MAX_ROUNDS = 10;

/**
 * Feeds RoundData from the live session: reshapes SkullKingSessionContext.state,
 * owns the drafts + debounced saves, and wires the phase-advance action.
 */
export function LiveRoundDataProvider({ children }: { children: React.ReactNode }) {
  const { state } = useSkullKingSessionContext();
  const { room, currentPlayer } = useRoomContext();
  const { setErrorMessage } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [bidDrafts, setBidDrafts] = useState<Map<string, number>>(new Map());
  const [trickDrafts, setTrickDrafts] = useState<Map<string, number>>(new Map());
  const [bonusDrafts, setBonusDrafts] = useState<Map<string, TeamBonus>>(new Map());

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

  const setBonus = (teamId: string, bonus: TeamBonus): void => {
    setBonusDrafts(prev => new Map(prev).set(teamId, bonus));
    schedule(teamId, () => submitBonusPoints(room.name, teamId, bonus, setErrorMessage));
  };

  const setKrakenPlayed = (value: boolean): void => {
    schedule("__kraken__", () => setKraken(room.name, state.round, value, setErrorMessage));
  };

  // --- Phase advance (admin only; each phase submits any stragglers, flushes, then transitions) ---

  const advanceBids = async (): Promise<void> => {
    const pending = state.teams.filter((t) => !(t.id in serverBids) && !bidDrafts.has(t.id));
    for (const t of pending) await submitBid(room.name, t.id, bids[t.id] ?? 0, setErrorMessage);
    await flush();
    await startRound(room.name, setErrorMessage);
  };

  const advanceTricks = async (): Promise<void> => {
    const pending = state.teams.filter((t) => !(t.id in serverTricks) && !trickDrafts.has(t.id));
    for (const t of pending) await submitTrickResult(room.name, t.id, trickResults[t.id] ?? 0, setErrorMessage);
    await flush();
    await startBonusPoints(room.name, setErrorMessage);
  };

  const advanceBonus = async (): Promise<void> => {
    const pending = state.teams.filter((t) => !(t.id in serverBonuses) && !bonusDrafts.has(t.id));
    for (const t of pending) {
      const eligible = bonusEligibility(bids[t.id], trickResults[t.id]).eligible;
      await submitBonusPoints(room.name, t.id, eligible ? (bonuses[t.id] ?? EMPTY_BONUS) : EMPTY_BONUS, setErrorMessage);
    }
    await flush();
    await finishRound(room.name, setErrorMessage);
  };

  const isAdmin = currentPlayer.role === "ADMIN";

  const advanceFor = (): { advance: (() => Promise<void>) | null; label: string | null } => {
    if (!isAdmin) return { advance: null, label: null };
    switch (state.gameState) {
      case "BIDS": return { advance: advanceBids, label: "Start round" };
      case "TRICK_RESULTS": return { advance: advanceTricks, label: "Continue to bonus points" };
      case "BONUS_POINTS":
        return { advance: advanceBonus, label: state.round >= MAX_ROUNDS ? "Finish game" : "Finish round" };
      default: return { advance: null, label: null };
    }
  };

  const { advance, label: advanceLabel } = advanceFor();

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
    krakenPlayed: state.krakenPlayed ?? false,
    setKrakenPlayed,
    advance,
    advanceLabel,
  };

  return <RoundDataContext.Provider value={value}>{children}</RoundDataContext.Provider>;
}

export function useRoundData(): RoundData {
  const ctx = useContext(RoundDataContext);
  if (!ctx) throw new Error("useRoundData must be used within a RoundData provider");
  return ctx;
}
