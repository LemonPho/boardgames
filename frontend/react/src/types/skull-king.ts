import type { TeamResponse } from "./teams";

export type SkullKingGameState = "BIDS" | "IN_PROGRESS" | "TRICK_RESULTS" | "BONUS_POINTS";

export interface TeamBonus {
  standardFourteens: number;
  blackFourteen: boolean;
  mermaidsByPirate: number;
  piratesBySkullKing: number;
  skullKingByMermaid: boolean;
  loot: number;
}

export interface SkullKingState {
  gameState: SkullKingGameState;
  round: number;
  cardCount: number;
  teams: TeamResponse[];
  startingTeamId: string;
  bids?: Record<string, number>;
  trickResults?: Record<string, number>;
  bonuses?: Record<string, TeamBonus>;
  krakenPlayed?: boolean;
  // Identity of the current round's BIDS event: changes on a new round AND on a reset, so
  // clients can drop round-local drafts.
  roundKey?: string | null;
  // Id of the current phase event. Saves send the id they were made in, so the server can
  // accept a change for the phase that just closed within its 1 s grace window.
  phaseEventId?: string;
  // Version of each team's value per field (bumped by every human edit). A save sends the
  // version it was made against, so the server can reject a save from an out-of-date screen.
  // A team missing from a map is at version 0. Players only get their own team's entry.
  bidVersions?: Record<string, number>;
  trickVersions?: Record<string, number>;
  bonusVersions?: Record<string, number>;
}

// Body of a 409 from a submit: the save was made against an older version, nothing was
// written, and this is the team's current value and version. The value fields are absent
// when the server has no row for the team yet (version 0, e.g. after a round reset).
export type VersionConflict =
  | { teamId: string; field: "BIDS"; version: number; bid?: number }
  | { teamId: string; field: "TRICK_RESULTS"; version: number; tricksWon?: number }
  | ({ teamId: string; field: "BONUS_POINTS"; version: number } & Partial<TeamBonus>);

export const EMPTY_BONUS: TeamBonus = {
  standardFourteens: 0,
  blackFourteen: false,
  mermaidsByPirate: 0,
  piratesBySkullKing: 0,
  skullKingByMermaid: false,
  loot: 0,
};

export function bonusTotal(b: TeamBonus): number {
  return b.standardFourteens * 10
    + (b.blackFourteen ? 20 : 0)
    + b.mermaidsByPirate * 20
    + b.piratesBySkullKing * 30
    + (b.skullKingByMermaid ? 40 : 0)
    + b.loot * 20;
}

export interface RoundHistoryTeam {
  teamId: string;
  playerName: string | null;
  bid: number | null;
  tricksWon: number | null;
  bonus: TeamBonus | null;
  roundScore: number;
}

export interface RoundHistory {
  round: number;
  cardCount: number;
  completed: boolean;
  krakenPlayed: boolean;
  startingTeamId: string;
  teams: RoundHistoryTeam[];
}

export interface BonusEligibility {
  eligible: boolean;
  reason?: string;
  // A made zero bid: only Loot can score (see bonusEligibility).
  lootOnly?: boolean;
}

// MIRRORS backend SkullKingService.bonusEligibility; keep in sync.
// A team that made a bid of one or more can earn every bonus, and a missed bid voids
// all bonuses. A made zero bid took no tricks, so it captured no cards (no 14s or
// capture bonuses), but it can still have played Loot into an alliance: Loot only.
export function bonusEligibility(bid: number | undefined, tricksWon: number | undefined): BonusEligibility {
  if (bid === undefined || tricksWon === undefined) {
    return { eligible: false, reason: "Waiting on this round's bid and tricks." };
  }
  if (bid !== tricksWon) {
    return { eligible: false, reason: "No bonus points — the bid wasn't met." };
  }
  if (bid === 0) {
    return { eligible: true, lootOnly: true };
  }
  return { eligible: true };
}

// MIRRORS the backend: SkullKingService.expectedTrickTotal / startBonusPoints trick-total
// check. If you change the rule here, change it there too (and vice versa).
// A Kraken destroys one trick (won by nobody), so the teams' tricks must sum to one
// fewer than the cards dealt.
export function expectedTrickTotal(cardCount: number, krakenPlayed: boolean): number {
  return cardCount - (krakenPlayed ? 1 : 0);
}
