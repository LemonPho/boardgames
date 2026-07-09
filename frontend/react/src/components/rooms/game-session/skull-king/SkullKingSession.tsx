import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";
import BidPhase from "./bid-phase/BidPhase";

export default function () {
  const { gameState } = useSkullKingSessionContext();

  if(gameState == "BIDS") return <BidPhase/>
}