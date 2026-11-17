import { useRoomContext } from "../../../../../../context/RoomContext";
import { useRoundData } from "../../../../../../context/RoundDataContext";
import { PlayerCounterCard } from "../../shared/PlayerCounterCard";
import { KrakenToggle } from "../../shared/KrakenToggle";
import AnonymousTeamsSection from "../../shared/AnonymousTeamsSection";

export default function PlayerView() {
  const { currentPlayer } = useRoomContext();
  const { round, cardCount, trickResults, canEdit, trickStatus, setTricks, startingTeamId, advancedCards, krakenPlayed, setKrakenPlayed } = useRoundData();

  if (!currentPlayer || !currentPlayer.team) return null;

  const teamId = currentPlayer.team.id;
  const value = trickResults[teamId] ?? 0;
  const editable = canEdit(teamId);
  // An admin here is in Player view (SELF room). They keep the round-level Kraken toggle
  // and still enter anonymous players' values.
  const isAdmin = currentPlayer.role === "ADMIN";

  return (
    <div>
      {isAdmin && advancedCards && (
        <div className="mb-4">
          <KrakenToggle krakenPlayed={krakenPlayed} editable onToggle={setKrakenPlayed} />
        </div>
      )}
      <PlayerCounterCard
        title="Tricks won"
        value={value}
        round={round}
        cardCount={cardCount}
        status={trickStatus(teamId)}
        leads={teamId === startingTeamId}
        onIncrement={editable ? () => setTricks(teamId, value + 1) : undefined}
        onDecrement={editable ? () => setTricks(teamId, value - 1) : undefined}
      />
      {isAdmin && <AnonymousTeamsSection phase="tricks" />}
    </div>
  );
}
