import { createContext, useContext, useEffect, useState } from "react";
import { useSessionContext } from "./SessionContext";
import type { SkullKingPayload } from "../types/skull-king";

interface SkullKingSessionType{
  gameState: string,
  round: number,
  cardCount: number
}

const SkullKingContext = createContext<SkullKingSessionType | null>(null);

export function SkullKingContextProvider({ children }: { children: React.ReactNode }){
  const { currentSessionEvent } = useSessionContext();

  const [round, setRound] = useState<number>(1);
  const [cardCount, setCardCount] = useState<number>(1);
  const [payload, setPayload] = useState<SkullKingPayload>();
  const [gameState, setGameState] = useState<"BIDS" | "PLAYING" | "SUBMIT_TRICKS" | "SUBMIT_BONUS_POINTS">("BIDS");

  const processSessionEvent = (): void => {
    if(!currentSessionEvent) return;

    const temp = currentSessionEvent.payload as SkullKingPayload;

    if(currentSessionEvent.type == "BIDS"){ 
      setGameState("BIDS");
    }

    setRound(temp.round);
    setCardCount(temp.cardCount);

  }

  useEffect(() => {
    if(!currentSessionEvent) return;

    processSessionEvent();
  }, [currentSessionEvent]);

  return(
    <SkullKingContext.Provider 
      value={{ gameState, round, cardCount }}
    >
      {children}
    </SkullKingContext.Provider>
  );
}

export function useSkullSkingSessionContext(){
  const ctx = useContext(SkullKingContext);
  if(!ctx) throw new Error("Skull king context error");
  return ctx;
}