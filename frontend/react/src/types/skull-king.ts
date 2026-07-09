export interface BidsPayload {
  round: number,
  cardCount: number
}

export interface InProgressPayload {
}

export interface TrickResultsPayload {
}

export interface BonusPointsPayload {
}

export type SkullKingPayload = BidsPayload | InProgressPayload | TrickResultsPayload | BonusPointsPayload;