import axios from "axios";
import type { RoundHistory, SkullKingState, TeamBonus, VersionConflict } from "../types/skull-king";
import type { ScoreboardResponse } from "../types/scoreboard";
import { setAxiosError } from "../util/api"
import { reviveDates } from "../util/dates";
import { api } from "./axiosSetup";

// A submit was made against an out-of-date version (409). Not an error for the user: the
// caller silently adopts the server's value, so no toast is shown for it.
export class VersionConflictError extends Error {
  conflict: VersionConflict;

  constructor(conflict: VersionConflict) {
    super("Version conflict");
    this.name = "VersionConflictError";
    this.conflict = conflict;
  }
}

// Shared error path of the versioned submits: a 409 becomes a VersionConflictError with no
// toast; everything else is toasted as usual. Always throws.
const rethrowSubmitError = (error: unknown, setErrorMessage: (message: string) => void): never => {
  if (axios.isAxiosError(error) && error.response?.status === 409) {
    throw new VersionConflictError(error.response.data as VersionConflict);
  }
  setAxiosError(error, setErrorMessage);
  throw error;
};

export const getScoreboard = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<ScoreboardResponse> => {
  try {
    const response = await api.get(`/skull-king/${roomName}/scoreboard`);
    // endedAt arrives as an ISO string; revive it into a Date.
    return reviveDates(response.data as ScoreboardResponse, "endedAt");
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const getSkullKingState = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<SkullKingState> => {
  try {
    const response = await api.get(`/skull-king/${roomName}/state`);
    return response.data as SkullKingState;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const getRoundHistory = async (
  roomName: string,
  round: number,
  setErrorMessage: (message: string) => void
): Promise<RoundHistory> => {
  try {
    const response = await api.get(`/skull-king/${roomName}/rounds/${round}`);
    return response.data as RoundHistory;
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const correctBids = async (
  roomName: string,
  round: number,
  teams: { teamId: string; value: number }[],
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/rounds/${round}/bids`, { teams });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const correctTricks = async (
  roomName: string,
  round: number,
  teams: { teamId: string; value: number }[],
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/rounds/${round}/tricks`, { teams });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const correctBonus = async (
  roomName: string,
  round: number,
  teams: (TeamBonus & { teamId: string })[],
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/rounds/${round}/bonus`, { teams });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const setKraken = async (
  roomName: string,
  round: number,
  krakenPlayed: boolean,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/rounds/${round}/kraken`, { krakenPlayed });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const submitBid = async (
  roomName: string,
  teamId: string,
  bid: number,
  setErrorMessage: (message: string) => void,
  // The phase the edit was made in (see SkullKingState.phaseEventId).
  phaseEventId?: string,
  // The version the edit was made against; omitted = no check. A mismatch is a 409, thrown
  // as a VersionConflictError without a toast.
  expectedVersion?: number
): Promise<number> => {
  try {
    const response = await api.post(`/skull-king/${roomName}/bids`, { teamId, bid, phaseEventId, expectedVersion });
    // The row's new version.
    return (response.data as { version: number }).version;
  } catch (error) {
    return rethrowSubmitError(error, setErrorMessage);
  }
}

export const submitTrickResult = async (
  roomName: string,
  teamId: string,
  tricksWon: number,
  setErrorMessage: (message: string) => void,
  // The phase the edit was made in (see SkullKingState.phaseEventId).
  phaseEventId?: string,
  // The version the edit was made against; omitted = no check. A mismatch is a 409, thrown
  // as a VersionConflictError without a toast.
  expectedVersion?: number
): Promise<number> => {
  try {
    const response = await api.post(`/skull-king/${roomName}/trick-results`, { teamId, tricksWon, phaseEventId, expectedVersion });
    // The row's new version.
    return (response.data as { version: number }).version;
  } catch (error) {
    return rethrowSubmitError(error, setErrorMessage);
  }
}

export const startRound = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/start-round`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const startTrickResults = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/start-trick-results`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const startBonusPoints = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/start-bonus-points`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const submitBonusPoints = async (
  roomName: string,
  teamId: string,
  bonus: TeamBonus,
  setErrorMessage: (message: string) => void,
  // The phase the edit was made in (see SkullKingState.phaseEventId).
  phaseEventId?: string,
  // The version the edit was made against; omitted = no check. A mismatch is a 409, thrown
  // as a VersionConflictError without a toast.
  expectedVersion?: number
): Promise<number> => {
  try {
    const response = await api.post(`/skull-king/${roomName}/bonus-points`, { teamId, ...bonus, phaseEventId, expectedVersion });
    // The row's new version.
    return (response.data as { version: number }).version;
  } catch (error) {
    return rethrowSubmitError(error, setErrorMessage);
  }
}

export const finishRound = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/finish-round`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const resetRound = async (
  roomName: string,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/reset-round`);
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}
