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
  bids?: Record<string, number>;
  trickResults?: Record<string, number>;
  bonuses?: Record<string, TeamBonus>;
  krakenPlayed?: boolean;
}

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
  teams: RoundHistoryTeam[];
}

export interface BonusEligibility {
  eligible: boolean;
  reason?: string;
}

// A team can earn bonus points only if it made a bid of one or more. A zero bid
// takes no tricks, so no cards are captured, and a missed bid voids all bonuses.
export function bonusEligibility(bid: number | undefined, tricksWon: number | undefined): BonusEligibility {
  if (bid === undefined || tricksWon === undefined) {
    return { eligible: false, reason: "Waiting on this round's bid and tricks." };
  }
  if (bid !== tricksWon) {
    return { eligible: false, reason: "No bonus points — the bid wasn't met." };
  }
  if (bid === 0) {
    return { eligible: false, reason: "No bonus points — a zero bid takes no tricks." };
  }
  return { eligible: true };
}
