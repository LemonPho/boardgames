import { useEffect, useState } from "react";
import { getRoundHistory } from "../../../../api/skullKing";
import { bonusTotal, type RoundHistory } from "../../../../types/skull-king";
import { useAlertsContext } from "../../../../context/AlertsContext";

interface RoundHistoryViewProps {
  roomName: string;
  round: number;
  onBack: () => void;
}

export default function RoundHistoryView({ roomName, round, onBack }: RoundHistoryViewProps) {
  const { setErrorMessage } = useAlertsContext();
  const [history, setHistory] = useState<RoundHistory | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    const fetch = async () => {
      try {
        setLoading(true);
        const data = await getRoundHistory(roomName, round, setErrorMessage);
        if (!cancelled) setHistory(data);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    fetch();
    return () => { cancelled = true; };
  }, [roomName, round]);

  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <button
          type="button"
          onClick={onBack}
          className="text-sm text-gray-500 hover:text-gray-800 transition-colors"
        >
          ← Rounds
        </button>
        <span className="text-sm font-semibold text-gray-800">
          Round {round}
          {history && ` · ${history.cardCount} cards`}
        </span>
      </div>

      {loading ? (
        <p className="text-sm text-gray-500">Loading…</p>
      ) : !history ? (
        <p className="text-sm text-gray-500">Couldn't load this round.</p>
      ) : (
        <div className="flex flex-col gap-1">
          {/* Column headers */}
          <div className="flex items-center gap-2 px-3 text-[11px] font-medium uppercase tracking-wide text-gray-400">
            <span className="flex-1">Player</span>
            <span className="w-10 text-center">Bid</span>
            <span className="w-10 text-center">Won</span>
            <span className="w-12 text-center">Bonus</span>
            <span className="w-14 text-right">Score</span>
          </div>

          {history.teams.map((team) => (
            <div
              key={team.teamId}
              className="flex items-center gap-2 px-3 py-2 rounded-xl border border-gray-100 text-sm"
            >
              <span className="flex-1 truncate text-gray-800">{team.playerName}</span>
              <span className="w-10 text-center tabular-nums">{team.bid ?? "–"}</span>
              <span className="w-10 text-center tabular-nums">{team.tricksWon ?? "–"}</span>
              <span className="w-12 text-center tabular-nums text-gray-500">
                {team.bonus ? `+${bonusTotal(team.bonus)}` : "–"}
              </span>
              <span
                className={`w-14 text-right font-semibold tabular-nums ${
                  team.roundScore < 0 ? "text-red-500" : "text-gray-800"
                }`}
              >
                {team.roundScore > 0 ? `+${team.roundScore}` : team.roundScore}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
