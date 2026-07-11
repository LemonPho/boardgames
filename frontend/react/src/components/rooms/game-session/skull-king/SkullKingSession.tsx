import { useSkullKingSessionContext } from "../../../../context/SkullKingSessionContext";
import BidPhase from "./bid-phase/BidPhase";
import RoundInProgressPhase from "./round-in-progress-phase/RoundInProgressPhase";
import BonusPointsPhase from "./round-results-phase/bonus-points-phase/BonusPointsPhase";
import TricksPhase from "./round-results-phase/tricks-phase/TricksPhase";

export default function () {
  const { state } = useSkullKingSessionContext();

  if (!state) return null;

  if(state.gameState == "BIDS") return <BidPhase/>
  if(state.gameState == "IN_PROGRESS") return <RoundInProgressPhase/>
  if(state.gameState == "TRICK_RESULTS") return <TricksPhase/>
  if(state.gameState == "BONUS_POINTS") return <BonusPointsPhase/>

  return(
    <div>Unkown state</div>
  );
}
