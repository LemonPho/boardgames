import { useSkullSkingSessionContext } from "../../../../context/SkullKingSessionContext";
import BidPhase from "./bid-phase/BidPhase";

export default function () {
  const { gameState } = useSkullSkingSessionContext();

  if(gameState == "BIDS") return <BidPhase/>
}