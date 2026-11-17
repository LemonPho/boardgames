import { useEffect, useState } from "react";
import { useParams, useLocation, useNavigate, Link } from "react-router-dom";
import { Crown, ArrowLeft } from "lucide-react";
import type { ScoreboardResponse, ScoreboardTeam } from "../../types/scoreboard";
import type { RoomUserResponse } from "../../types/rooms";
import { getScoreboard } from "../../api/skullKing";
import { recreateRoom } from "../../api/rooms";
import { useAlertsContext } from "../../context/AlertsContext";
import { useOptionalRoomContext } from "../../context/RoomContext";
import SubmitButton from "../util/SubmitButton";

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
 * user's match history. Also rendered by RoomPage for a COMPLETED room, where it
 * adds the end-of-game actions (Return to room / Join next game / Exit).
 */
export default function FinalScoreboard() {
  const { name } = useParams();
  const { setErrorMessage, setInfoMessage } = useAlertsContext();
  const location = useLocation();
  const navigate = useNavigate();
  const navState = (location.state as ScoreboardNavState | null) ?? {};
  const backTo = navState.backTo ?? "/";
  const backLabel = navState.backLabel ?? "Home";

  const [scoreboard, setScoreboard] = useState<ScoreboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [returning, setReturning] = useState(false);

  // Null on the /scoreboard/:name history route (no RoomContextProvider there).
  const roomCtx = useOptionalRoomContext();
  const liveRoom = roomCtx?.room ?? null;
  // Only the live finished room gets the end-of-game actions; match history stays read-only.
  const inRoom = liveRoom !== null && liveRoom.name === name && liveRoom.status === "COMPLETED";
  const isAdmin = roomCtx?.currentPlayer?.role === "ADMIN";
  // Right after navigating /rooms/A -> /rooms/B the provider still holds room A for a render
  // before its [name] effect starts loading B. Don't fetch a scoreboard for B (it has none
  // yet, which would flash a "not found" error toast) during that handover.
  const switchingRoom = liveRoom !== null && liveRoom.name !== name;

  useEffect(() => {
    if (!name || switchingRoom) return;
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
  }, [name, switchingRoom]);

  const handleReturnToRoom = async (): Promise<void> => {
    if (!name || !liveRoom) return;
    const next = await recreateRoom(name, setErrorMessage);
    // The backend skips players who are already in another live room; tell the admin who.
    const skipped = liveRoom.players
      .filter((p) => p.status === "ACTIVE")
      .filter((p) => !next.players.some((np) => sameSeatPerson(np, p)))
      .map((p) => (p.role === "ANONYMOUS" ? p.displayName : p.user?.username));
    if (skipped.length) setInfoMessage(`${skipped.join(", ")} couldn't be added (already in another room)`);
    navigate(`/rooms/${next.name}`);
  };

  if (switchingRoom) return null;

  if (loading) {
    return <p className="w-full max-w-lg text-sm text-gray-400">Loading…</p>;
  }

  if (!scoreboard) {
    return <p className="w-full max-w-lg text-sm text-gray-500">Scoreboard not found.</p>;
  }

  const winner = scoreboard.teams.find((t) => t.won);

  return (
    <div className="w-full max-w-lg">
      {/* In the live room, the Exit button below replaces the back link. */}
      {!inRoom && (
        <Link to={backTo} className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 mb-6">
          <ArrowLeft size={15} /> {backLabel}
        </Link>
      )}

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
          <StandingsRow key={team.teamId} team={team} />
        ))}
      </div>

      {inRoom && (
        <div className="mt-8 flex flex-col gap-2">
          {isAdmin ? (
            <SubmitButton
              text="Return to room"
              loading={returning}
              setLoading={setReturning}
              onSubmit={handleReturnToRoom}
              className={PRIMARY_BUTTON}
            />
          ) : liveRoom.nextRoomName ? (
            // Appears live: the old room's WebSocket push carries nextRoomName.
            <button
              type="button"
              onClick={() => navigate(`/rooms/${liveRoom.nextRoomName}`)}
              className={PRIMARY_BUTTON}
            >
              Join next game
            </button>
          ) : (
            <p className="text-center text-sm text-gray-400">Waiting for the host to return to the room…</p>
          )}
          <button
            type="button"
            onClick={() => navigate("/")}
            className="w-full h-11 rounded-lg text-sm font-medium border border-gray-200 text-gray-700 transition active:scale-[0.98]"
          >
            Exit
          </button>
        </div>
      )}
    </div>
  );
}

const PRIMARY_BUTTON =
  "w-full h-11 rounded-lg text-sm font-medium bg-neutral-900 text-white disabled:opacity-40 transition active:scale-[0.98]";

// Whether two room members (from different rooms) are the same person: registered players by
// username, anonymous ones by display name.
function sameSeatPerson(a: RoomUserResponse, b: RoomUserResponse): boolean {
  if (a.role === "ANONYMOUS" || b.role === "ANONYMOUS") {
    return a.role === b.role && a.displayName === b.displayName;
  }
  return a.user?.username === b.user?.username;
}

/**
 * One standings row. Only a live account has a profile to visit, so anonymous and
 * deleted players render as plain rows rather than dead links.
 */
function StandingsRow({ team }: { team: ScoreboardTeam }) {
  const className = `flex items-center gap-3 px-4 py-3 rounded-xl border ${
    team.won ? "border-amber-200 bg-amber-50" : "border-gray-200 bg-white"
  }`;

  const contents = (
    <>
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
      {team.playerType === "DELETED" && (
        <span className="shrink-0 text-xs text-gray-400">Deleted</span>
      )}
      <p className="text-sm font-semibold text-gray-900">{team.score}</p>
    </>
  );

  if (team.playerType !== "ACTIVE" || !team.username) {
    return <div className={className}>{contents}</div>;
  }

  // An absolute path: this page is mounted at two depths (/scoreboard/:name and
  // /rooms/:name/final), so a relative one would resolve against the current
  // route and append instead of navigating to the profile.
  return (
    <Link to={`/profile/${team.username}`} className={`${className} hover:border-gray-300 transition-colors`}>
      {contents}
    </Link>
  );
}

function formatDate(date: Date): string {
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}
