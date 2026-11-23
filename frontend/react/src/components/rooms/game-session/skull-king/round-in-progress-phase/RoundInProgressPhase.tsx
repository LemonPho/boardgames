import { useRoomContext } from "../../../../../context/RoomContext";
import { useSkullKingSessionContext } from "../../../../../context/SkullKingSessionContext";
import { MyBidCard } from "../shared/MyBidCard";
import { LeaderNotice } from "../shared/LeaderNotice";
import { AdminCounterCard } from "../shared/AdminCounterCard";

// The admin's "Enter trick results" lives in the session's AdminActionBar
// (RoundData.advance), so this phase shows the player's own bid, or (toggled from the
// header) everyone's bids as a read-only admin-style grid. Players receive all bids from
// IN_PROGRESS onward; they're only secret while bidding.
export default function RoundInProgressPhase({ showAllBids = false }: { showAllBids?: boolean }) {
  const { room, currentPlayer } = useRoomContext();
  const { state } = useSkullKingSessionContext();

  if (!room || !currentPlayer || !state) return null;

  const bids = state.bids ?? {};
  const myBid = currentPlayer.team ? bids[currentPlayer.team.id] ?? null : null;

  if (showAllBids) {
    const totalBids = state.teams.reduce((sum, team) => sum + (bids[team.id] ?? 0), 0);
    return (
      <div className="p-4 max-w-md mx-auto">
        <h2 className="text-lg font-medium text-center mb-6">Bids</h2>
        <div className="grid grid-cols-2 gap-3 mb-3">
          {state.teams.map((team) => (
            <AdminCounterCard
              key={team.id}
              playerName={team.player.displayName}
              editable={false}
              team={team}
              value={bids[team.id] ?? 0}
              max={state.cardCount}
              leads={team.id === state.startingTeamId}
              change={() => {}}
            />
          ))}
        </div>
        <p className="text-center text-sm text-neutral-500">
          Total bids {totalBids} · {state.cardCount} cards
        </p>
      </div>
    );
  }

  return (
    <div className="p-4 max-w-md mx-auto">
      <LeaderNotice teams={state.teams} startingTeamId={state.startingTeamId} myTeamId={currentPlayer.team?.id} />
      <MyBidCard
        round={state.round}
        cardCount={state.cardCount}
        bid={myBid}
        leads={currentPlayer.team ? currentPlayer.team.id === state.startingTeamId : false}
      />
    </div>
  );
}
