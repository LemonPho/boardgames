import { createContext, useContext, useEffect, useRef, useState } from "react";
import type { TeamResponse } from "../types/teams";
import type { BonusEligibility, SkullKingGameState, SkullKingState, TeamBonus, VersionConflict } from "../types/skull-king";
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
  VersionConflictError,
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

// How long a successful save waits for the WebSocket push carrying its value before
// falling back to a REST re-fetch. A push that disagrees (another client's write won) or
// one that was lost would otherwise leave "Saving…" up forever.
const CONFIRM_TIMEOUT_MS = 2000;

const KRAKEN_KEY = "__kraken__";

// The versioned per-team fields (the Kraken flag isn't versioned).
type VersionedField = "bids" | "tricks" | "bonuses";
type KnownVersions = Record<VersionedField, Map<string, number>>;

const FIELD_OF_CONFLICT: Record<VersionConflict["field"], VersionedField> = {
  BIDS: "bids",
  TRICK_RESULTS: "tricks",
  BONUS_POINTS: "bonuses",
};

// A save that reached the server, waiting for the state to show its value.
interface AwaitingConfirm {
  gen: number;
  confirmed: (s: SkullKingState) => boolean;
  // Drops the draft and clears "Saving…": the server value is shown from then on.
  settle: () => void;
  timer: ReturnType<typeof setTimeout>;
}

// Map/Set copies with one entry removed, returning the same instance when there's
// nothing to remove so React can skip the re-render.
const withoutKey = <T,>(map: Map<string, T>, key: string): Map<string, T> => {
  if (!map.has(key)) return map;
  const next = new Map(map);
  next.delete(key);
  return next;
};
const withoutItem = (set: Set<string>, key: string): Set<string> => {
  if (!set.has(key)) return set;
  const next = new Set(set);
  next.delete(key);
  return next;
};

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
  const { state, refreshState, applyServerValue } = useSkullKingSessionContext();
  const { room, currentPlayer } = useRoomContext();
  const { setErrorMessage, clearAlerts } = useAlertsContext();
  const { schedule, flush } = useDebouncedSave();

  const [bidDrafts, setBidDrafts] = useState<Map<string, number>>(new Map());
  const [trickDrafts, setTrickDrafts] = useState<Map<string, number>>(new Map());
  const [bonusDrafts, setBonusDrafts] = useState<Map<string, TeamBonus>>(new Map());
  // Optimistic Kraken value: the toggle reflects the tap immediately instead of
  // waiting for debounce + POST + WebSocket push. null = no local edit, use server.
  const [krakenDraft, setKrakenDraft] = useState<boolean | null>(null);
  // Keys (team ids / "__kraken__") with a save scheduled, in flight, or awaiting the
  // server's confirmation. "Saving…" is shown only for these.
  const [savingKeys, setSavingKeys] = useState<Set<string>>(new Set());

  // Save lifecycle bookkeeping lives in refs, not state: scheduled saves run long after
  // the render that created them, so they must read the *current* values, not the ones
  // their closure captured.
  // - gens: per-key edit counter. A save captures its generation when scheduled and only
  //   touches the draft/status if it's still the latest, so a stale save's completion
  //   never clears a newer edit.
  // - awaiting: saves that succeeded and wait for the state to confirm their value.
  // - latestState: the newest server state. A push can arrive before the POST response,
  //   so a save checks it as soon as the request resolves.
  // - mounted: saves flushed on unmount still complete; they mustn't start new timers.
  const gens = useRef(new Map<string, number>());
  // Per key, the newest generation whose save must NOT be sent: set on a version conflict.
  // Edits queued behind the rejected one were computed from the same out-of-date value, so
  // sending them (now with the fresh version) would overwrite the newer server value.
  const invalidatedThrough = useRef(new Map<string, number>());
  const awaiting = useRef(new Map<string, AwaitingConfirm>());
  const latestState = useRef<SkullKingState | null>(state);
  const mounted = useRef(false);

  // Every new state (push or REST refresh) settles the saves whose value it confirms.
  // Unconfirmed ones keep waiting for their timeout, which re-fetches.
  useEffect(() => {
    latestState.current = state;
    if (!state) return;
    awaiting.current.forEach((entry, key) => {
      if (!entry.confirmed(state)) return;
      clearTimeout(entry.timer);
      awaiting.current.delete(key);
      entry.settle();
    });
  }, [state]);

  // Sets mounted in the body too, not just at init: StrictMode runs this cleanup and then
  // the effect again on the same refs.
  useEffect(() => {
    mounted.current = true;
    const entries = awaiting.current;
    return () => {
      mounted.current = false;
      entries.forEach((entry) => clearTimeout(entry.timer));
      entries.clear();
    };
  }, []);

  // The newest version seen per team and field, from states and from save replies. A save
  // reads it when it's *sent*, not when tapped: saves for one team are chained, so the reply
  // to the previous one has already moved it on and a user's own fast taps never conflict.
  // An out-of-date screen still sends its old version and is rejected, which is the point.
  // Only ever moves forward, so a late push carrying an older version can't undo a reply.
  const knownVersions = useRef<KnownVersions>({ bids: new Map(), tricks: new Map(), bonuses: new Map() });
  const recordVersion = (field: VersionedField, teamId: string, version: number): void => {
    const versions = knownVersions.current[field];
    if (version > (versions.get(teamId) ?? 0)) versions.set(teamId, version);
  };
  const knownVersion = (field: VersionedField, teamId: string): number =>
    knownVersions.current[field].get(teamId) ?? 0;

  useEffect(() => {
    if (!state) return;
    const merge = (field: VersionedField, versions: Record<string, number> | undefined) => {
      const known = knownVersions.current[field];
      Object.entries(versions ?? {}).forEach(([teamId, version]) => {
        if (version > (known.get(teamId) ?? 0)) known.set(teamId, version);
      });
    };
    merge("bids", state.bidVersions);
    merge("tricks", state.trickVersions);
    merge("bonuses", state.bonusVersions);
  }, [state]);

  // The admin advanced: send any pending (debounced) saves right away, tagged with the
  // phase they were made in, so they reach the server inside its 1 s grace window. The ref
  // holds the phase last seen, so the first state (and every re-render with an unchanged
  // phase) doesn't flush. flush isn't stable across renders, hence the explicit comparison.
  const seenPhaseEventId = useRef(state?.phaseEventId);
  useEffect(() => {
    const phaseEventId = state?.phaseEventId;
    if (phaseEventId === seenPhaseEventId.current) return;
    const isFirst = seenPhaseEventId.current === undefined;
    seenPhaseEventId.current = phaseEventId;
    if (!isFirst) void flush();
  }, [state?.phaseEventId, flush]);

  const latestGen = (key: string): number => gens.current.get(key) ?? 0;

  const markDone = (key: string): void => setSavingKeys(prev => withoutItem(prev, key));

  // Called synchronously by every setter, before scheduling: a new generation, and
  // "Saving…" right away, which covers the debounce window. The key's previous save, if
  // still awaiting confirmation, is superseded, so its timer goes.
  const beginEdit = (key: string): number => {
    const gen = latestGen(key) + 1;
    gens.current.set(key, gen);
    const previous = awaiting.current.get(key);
    if (previous) {
      clearTimeout(previous.timer);
      awaiting.current.delete(key);
    }
    setSavingKeys(prev => (prev.has(key) ? prev : new Set(prev).add(key)));
    return gen;
  };

  // The save reached the server. Settle now if the state already shows its value;
  // otherwise wait for a push that does, or time out, re-fetch, and take the server's
  // value either way. The server always wins in the end, which resolves collisions.
  const awaitConfirm = (
    key: string,
    gen: number,
    confirmed: (s: SkullKingState) => boolean,
    dropDraft: () => void,
  ): void => {
    if (!mounted.current) return;
    const settle = () => { dropDraft(); markDone(key); };
    const current = latestState.current;
    if (current && confirmed(current)) {
      settle();
      return;
    }
    const timer = setTimeout(async () => {
      await refreshState();
      // A push (or the refresh itself) may have confirmed it, or a new edit superseded it,
      // while the request was out.
      if (awaiting.current.get(key)?.gen !== gen) return;
      awaiting.current.delete(key);
      settle();
    }, CONFIRM_TIMEOUT_MS);
    awaiting.current.set(key, { gen, confirmed, settle, timer });
  };

  // Wraps a scheduled save with the lifecycle above. On rejection (wrong phase, out of
  // range, the trick cap) the draft is dropped at once, so the control reverts to the
  // server value next to the server's toast. The error is rethrown for flush().
  // A version conflict isn't a failure: the screen was out of date, so it silently adopts
  // the server's value (no toast, by design) and flush() carries on.
  const trackedSave = (
    key: string,
    gen: number,
    send: () => Promise<void>,
    confirmed: (s: SkullKingState) => boolean,
    dropDraft: () => void,
  ) => async (): Promise<void> => {
    // Superseded by a version conflict before it was sent: drop it (see invalidatedThrough).
    if (gen <= (invalidatedThrough.current.get(key) ?? 0)) return;
    try {
      await send();
      if (latestGen(key) !== gen) return; // a newer edit owns this key now
      awaitConfirm(key, gen, confirmed, dropDraft);
    } catch (error) {
      if (error instanceof VersionConflictError) {
        const { conflict } = error;
        // After an unmount (a save flushed as the round changed), the state belongs to the
        // next round; patching it with this round's value would be wrong.
        if (mounted.current) applyServerValue(conflict);
        recordVersion(FIELD_OF_CONFLICT[conflict.field], conflict.teamId, conflict.version);
        // Edits queued behind this one were made on the same stale value: cancel them all,
        // so the corrected server value stands. Taps made after this point get a newer
        // generation and are sent normally (with the fresh version).
        invalidatedThrough.current.set(key, latestGen(key));
        dropDraft();
        markDone(key);
        return;
      }
      if (latestGen(key) === gen) {
        dropDraft();
        markDone(key);
      }
      throw error;
    }
  };

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

  // "Saving…" comes from the explicit lifecycle, not from comparing draft and server:
  // that comparison stuck forever whenever the server's final value differed from the
  // draft (a collision, a rejected save, a lost push).
  const statusFor = (teamId: string, server: Record<string, unknown>): SaveStatus => {
    if (savingKeys.has(teamId)) return "saving";
    if (teamId in server) return "saved";
    return null;
  };

  const bidStatus = (teamId: string) => statusFor(teamId, serverBids);
  const trickStatus = (teamId: string) => statusFor(teamId, serverTricks);
  const bonusStatus = (teamId: string) => statusFor(teamId, serverBonuses);

  const setBid = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    // Captured at tap time: the save may run after the phase has moved on.
    const phaseEventId = state.phaseEventId;
    const gen = beginEdit(teamId);
    setBidDrafts(prev => new Map(prev).set(teamId, value));
    schedule(teamId, trackedSave(
      teamId,
      gen,
      async () => {
        const version = await submitBid(
          room.name, teamId, value, setErrorMessage, phaseEventId, knownVersion("bids", teamId));
        recordVersion("bids", teamId, version);
      },
      s => s.bids?.[teamId] === value,
      () => setBidDrafts(prev => withoutKey(prev, teamId)),
    ));
  };

  const setTricks = (teamId: string, value: number): void => {
    if (value < 0 || value > state.cardCount) return;
    if (currentPlayer.role === "ADMIN") {
      // Mirrors the backend's running cap in submitTrickResult (sum of all teams ≤ cardCount):
      // refuse the tap instead of sending a save the server will reject.
      const others = state.teams.reduce((sum, t) => t.id === teamId ? sum : sum + (trickResults[t.id] ?? 0), 0);
      if (others + value > state.cardCount) return;
    }
    // Captured at tap time: the save may run after the phase has moved on.
    const phaseEventId = state.phaseEventId;
    const gen = beginEdit(teamId);
    setTrickDrafts(prev => new Map(prev).set(teamId, value));
    schedule(teamId, trackedSave(
      teamId,
      gen,
      async () => {
        const version = await submitTrickResult(
          room.name, teamId, value, setErrorMessage, phaseEventId, knownVersion("tricks", teamId));
        recordVersion("tricks", teamId, version);
      },
      s => s.trickResults?.[teamId] === value,
      () => setTrickDrafts(prev => withoutKey(prev, teamId)),
    ));
  };

  const setBonus = (teamId: string, value: TeamBonus): void => {
    // A zero bid can only earn Loot, and the backend rejects any other field. Stray
    // non-loot values can linger (e.g. a correction changed the bid from 2 to 0), so
    // strip them before saving.
    const bonus = bonusEligibilityFor(teamId).lootOnly ? { ...EMPTY_BONUS, loot: value.loot } : value;
    // Captured at tap time: the save may run after the phase has moved on.
    const phaseEventId = state.phaseEventId;
    const gen = beginEdit(teamId);
    setBonusDrafts(prev => new Map(prev).set(teamId, bonus));
    schedule(teamId, trackedSave(
      teamId,
      gen,
      async () => {
        const version = await submitBonusPoints(
          room.name, teamId, bonus, setErrorMessage, phaseEventId, knownVersion("bonuses", teamId));
        recordVersion("bonuses", teamId, version);
      },
      s => sameBonus(s.bonuses?.[teamId], bonus),
      () => setBonusDrafts(prev => withoutKey(prev, teamId)),
    ));
  };

  // The toggle is optimistic; on failure, or once the server confirms (or a re-fetch
  // settles it), the draft is dropped and the server's value shows.
  const setKrakenPlayed = (value: boolean): void => {
    const gen = beginEdit(KRAKEN_KEY);
    setKrakenDraft(value);
    schedule(KRAKEN_KEY, trackedSave(
      KRAKEN_KEY,
      gen,
      () => setKraken(room.name, state.round, value, setErrorMessage),
      s => (s.krakenPlayed ?? false) === value,
      () => setKrakenDraft(null),
    ));
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
    // Teams without a bonus are defaulted to empty by the server inside finishRound (like
    // bids/tricks), so advancing is a flush plus one request.
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

/**
 * Supplies a ready-made RoundData value. Lets other sources (e.g. the admin's review of an
 * earlier phase of the current round) feed the same phase components as the live game.
 */
export function RoundDataProvider({ value, children }: { value: RoundData; children: React.ReactNode }) {
  return <RoundDataContext.Provider value={value}>{children}</RoundDataContext.Provider>;
}

export function useRoundData(): RoundData {
  const ctx = useContext(RoundDataContext);
  if (!ctx) throw new Error("useRoundData must be used within a RoundData provider");
  return ctx;
}
