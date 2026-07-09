import { createContext, useContext, useEffect, useState } from "react";
import { useSessionContext } from "./SessionContext";
import type { BidsPayload } from "../types/skull-king";
import type { TeamSessionEventResponse } from "../types/sessions";
import { parseBidEvent, parseBonusPointEvent, parseTrickResultEvent, type ParsedBidEvent, type ParsedBonusPointEvent, type ParsedTrickResultEvent } from "../util/team-session-events";

export type SkullKingGameState = "BIDS" | "IN_PROGRESS" | "TRICK_RESULTS" | "BONUS_POINTS";

interface SkullKingSessionType {
  gameState: SkullKingGameState;
  round: number;
  cardCount: number;
  bids: Map<string, ParsedBidEvent>;
  trickResults: Map<string, ParsedTrickResultEvent>;
  bonusPoints: Map<string, ParsedBonusPointEvent>;
}

const SkullKingContext = createContext<SkullKingSessionType | null>(null);

export function SkullKingContextProvider({ children }: { children: React.ReactNode }) {
  const { currentSessionEvent, teamEvents } = useSessionContext();

  const [round, setRound] = useState<number>(1);
  const [cardCount, setCardCount] = useState<number>(1);
  const [gameState, setGameState] = useState<SkullKingGameState>("BIDS");

  const [bids, setBids] = useState<Map<string, ParsedBidEvent>>(new Map());
  const [trickResults, setTrickResults] = useState<Map<string, ParsedTrickResultEvent>>(new Map());
  const [bonusPoints, setBonusPoints] = useState<Map<string, ParsedBonusPointEvent>>(new Map());

  const processSessionEvent = (): void => {
    if (!currentSessionEvent) return;

    switch (currentSessionEvent.type) {
      case "BIDS": {
        const payload = currentSessionEvent.payload as BidsPayload;
        setGameState("BIDS");
        setRound(payload.round);
        setCardCount(payload.cardCount);
        setBids(new Map());
        break;
      }
      case "IN_PROGRESS": {
        setGameState("IN_PROGRESS");
        break;
      }
      case "TRICK_RESULTS": {
        setGameState("TRICK_RESULTS");
        setTrickResults(new Map());
        break;
      }
      case "BONUS_POINTS": {
        setGameState("BONUS_POINTS");
        setBonusPoints(new Map());
        break;
      }
    }
  }

  const processTeamEvent = (event: TeamSessionEventResponse): void => {
    switch (event.type) {
      case "BIDS": {
        const parsed = parseBidEvent(event);
        setBids(prev => new Map(prev).set(parsed.teamId, parsed));
        break;
      }
      case "TRICK_RESULTS": {
        const parsed = parseTrickResultEvent(event);
        setTrickResults(prev => new Map(prev).set(parsed.teamId, parsed));
        break;
      }
      case "BONUS_POINTS": {
        const parsed = parseBonusPointEvent(event);
        setBonusPoints(prev => new Map(prev).set(parsed.teamId, parsed));
        break;
      }
    }
  }

  useEffect(() => {
    if (!currentSessionEvent) return;
    processSessionEvent();
  }, [currentSessionEvent]);

  useEffect(() => {
    if (teamEvents.length === 0) return;

    const latestEvent = teamEvents[teamEvents.length - 1];
    processTeamEvent(latestEvent);
  }, [teamEvents]);

  return (
    <SkullKingContext.Provider
      value={{ gameState, round, cardCount, bids, trickResults, bonusPoints }}
    >
      {children}
    </SkullKingContext.Provider>
  );
}

export function useSkullKingSessionContext() {
  const ctx = useContext(SkullKingContext);
  if (!ctx) throw new Error("Skull king context error");
  return ctx;
}
