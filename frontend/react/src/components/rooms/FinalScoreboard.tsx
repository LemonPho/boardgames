import { useEffect, useState } from "react";
import { useParams, useLocation, Link } from "react-router-dom";
import { Crown, ArrowLeft } from "lucide-react";
import type { ScoreboardResponse } from "../../types/scoreboard";
import { getScoreboard } from "../../api/skullKing";
import { useAlertsContext } from "../../context/AlertsContext";

// Where the back button returns to, and its label. Passed via navigation state
// (e.g. from the profile match history); defaults to home when absent (e.g. a
// room finishing, or a direct visit).
interface ScoreboardNavState {
  backTo?: string;
  backLabel?: string;
}

/**
 * Final scoreboard for a room's session. Standalone (fetches its own data by the
 * room name in the URL), so it's reachable both when a room finishes and from a
 * user's match history. Also rendered by RoomPage for a COMPLETED room.
 */
export default function FinalScoreboard() {
  const { name } = useParams();
  const { setErrorMessage } = useAlertsContext();
  const location = useLocation();
  const navState = (location.state as ScoreboardNavState | null) ?? {};
  const backTo = navState.backTo ?? "/";
  const backLabel = navState.backLabel ?? "Home";

  const [scoreboard, setScoreboard] = useState<ScoreboardResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!name) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await getScoreboard(name, setErrorMessage);
        if (!cancelled) setScoreboard(data);
      } catch {
        if (!cancelled) setScoreboard(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [name]);

  if (loading) {
    return <p className="max-w-lg mx-auto px-4 py-8 text-sm text-gray-400">Loading…</p>;
  }

  if (!scoreboard) {
    return <p className="max-w-lg mx-auto px-4 py-8 text-sm text-gray-500">Scoreboard not found.</p>;
  }

  const winner = scoreboard.teams.find((t) => t.won);

  return (
    <div className="max-w-lg mx-auto px-4 py-8">
      <Link to={backTo} className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 mb-6">
        <ArrowLeft size={15} /> {backLabel}
      </Link>

      {/* Header */}
      <div className="text-center mb-8">
        <p className="text-xs font-semibold uppercase tracking-wide text-gray-400">
          {scoreboard.completed ? "Final results" : "Standings"}
        </p>
        <h1 className="text-2xl font-semibold text-gray-900 mt-1 break-words">{scoreboard.roomName}</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          {scoreboard.game}
          {scoreboard.endedAt ? ` · ${formatDate(scoreboard.endedAt)}` : ""}
        </p>
        {winner && (
          <div className="mt-4 inline-flex items-center gap-2 px-4 py-2 rounded-full bg-amber-100 text-amber-700">
            <Crown size={18} />
            <span className="text-sm font-semibold">{winner.playerName ?? "—"} wins</span>
          </div>
        )}
      </div>

      {/* Standings */}
      <div className="flex flex-col gap-2">
        {scoreboard.teams.map((team) => (
          <div
            key={team.teamId}
            className={`flex items-center gap-3 px-4 py-3 rounded-xl border ${
              team.won ? "border-amber-200 bg-amber-50" : "border-gray-200 bg-white"
            }`}
          >
            <div
              className={`w-9 h-9 shrink-0 rounded-full flex items-center justify-center text-sm font-semibold ${
                team.won ? "bg-amber-100 text-amber-700" : "bg-gray-100 text-gray-600"
              }`}
            >
              {team.won ? <Crown size={16} /> : `#${team.placement}`}
            </div>
            <p className="flex-1 min-w-0 text-sm font-medium text-gray-900 truncate">
              {team.playerName ?? "—"}
            </p>
            <p className="text-sm font-semibold text-gray-900">{team.score}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function formatDate(date: Date): string {
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
