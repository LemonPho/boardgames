import type { TeamSessionEventResponse } from "../types/sessions";

export interface ParsedBidEvent {
  id: string;
  teamId: string;
  bid: number;
  correctsEventId: string | null;
}

export interface ParsedTrickResultEvent {
  id: string;
  teamId: string;
  tricksWon: number;
  correctsEventId: string | null;
}

export interface ParsedBonusPointEvent {
  id: string;
  teamId: string;
  source: string;
  points: number;
  correctsEventId: string | null;
}

export function parseBidEvent(event: TeamSessionEventResponse): ParsedBidEvent {
  const payload = event.payload as { bid: number };
  return {
    id: event.id,
    teamId: event.teamId,
    bid: payload.bid,
    correctsEventId: event.correctsEventId,
  };
}

export function parseTrickResultEvent(event: TeamSessionEventResponse): ParsedTrickResultEvent {
  const payload = event.payload as { tricksWon: number };
  return {
    id: event.id,
    teamId: event.teamId,
    tricksWon: payload.tricksWon,
    correctsEventId: event.correctsEventId,
  };
}

export function parseBonusPointEvent(event: TeamSessionEventResponse): ParsedBonusPointEvent {
  const payload = event.payload as { source: string, points: number };
  return {
    id: event.id,
    teamId: event.teamId,
    source: payload.source,
    points: payload.points,
    correctsEventId: event.correctsEventId,
  };
}
