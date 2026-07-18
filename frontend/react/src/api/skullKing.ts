import type { RoundHistory, SkullKingState, TeamBonus } from "../types/skull-king";
import type { ScoreboardResponse } from "../types/scoreboard";
import { setAxiosError } from "../util/api"
import { reviveDates } from "../util/dates";
import { api } from "./axiosSetup";

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
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/bids`, { teamId, bid });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
  }
}

export const submitTrickResult = async (
  roomName: string,
  teamId: string,
  tricksWon: number,
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/trick-results`, { teamId, tricksWon });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
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
  setErrorMessage: (message: string) => void
): Promise<void> => {
  try {
    await api.post(`/skull-king/${roomName}/bonus-points`, { teamId, ...bonus });
  } catch (error) {
    setAxiosError(error, setErrorMessage);
    throw error;
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
