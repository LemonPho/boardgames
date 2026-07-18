import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { User as UserIcon, Trophy, Crown, Settings } from "lucide-react";
import type { UserResponse } from "../../types/user";
import type { MatchHistoryResponse } from "../../types/matches";
import { getUserByUsername, getMatchHistory } from "../../api/user";
import { useAlertsContext } from "../../context/AlertsContext";
import { useUserContext } from "../../context/UserContext";

/**
 * User profile page for /profile/:username — viewable for any user, not just
 * the logged-in one. Shows an identity header (icon, username, join date) and a
 * list of previous matches. Match history is mocked for now while the design
 * settles; the real data/endpoint comes later.
 */
export default function ProfilePage() {
  const { username } = useParams();
  const { setErrorMessage } = useAlertsContext();
  const { user } = useUserContext();

  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [matches, setMatches] = useState<MatchHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [matchesLoading, setMatchesLoading] = useState(true);

  useEffect(() => {
    if (!username) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
      try {
        const data = await getUserByUsername(username, setErrorMessage);
        if (!cancelled) setProfile(data);
      } catch {
        if (!cancelled) setProfile(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [username]);

  useEffect(() => {
    if (!username) return;
    let cancelled = false;
    (async () => {
      setMatchesLoading(true);
      try {
        const data = await getMatchHistory(username, setErrorMessage);
        if (!cancelled) setMatches(data);
      } catch {
        if (!cancelled) setMatches([]);
      } finally {
        if (!cancelled) setMatchesLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [username]);

  if (loading) {
    return <p className="max-w-2xl mx-auto px-4 sm:px-6 py-8 text-sm text-gray-400">Loading…</p>;
  }

  if (!profile) {
    return (
      <p className="max-w-2xl mx-auto px-4 sm:px-6 py-8 text-sm text-gray-500">
        User not found.
      </p>
    );
  }

  const joinedLabel = profile.createdAt
    ? new Date(profile.createdAt).toLocaleDateString(undefined, {
        year: "numeric",
        month: "long",
      })
    : "—";

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-8">
      {/* Identity header */}
      <div className="flex items-center gap-4 mb-8">
        <div className="w-16 h-16 sm:w-20 sm:h-20 shrink-0 rounded-full bg-gray-100 flex items-center justify-center">
          <UserIcon className="text-gray-400" size={32} />
        </div>
        <div className="min-w-0 flex-1">
          <h1 className="text-xl sm:text-2xl font-semibold text-gray-900 truncate">
            {profile.username}
          </h1>
          <p className="text-sm text-gray-500">Joined {joinedLabel}</p>
        </div>
        {/* Edit only shows on your own profile. */}
        {user?.username === profile.username && (
          <Link
            to="/settings"
            className="shrink-0 inline-flex items-center gap-1.5 text-sm font-medium px-3 py-2 rounded-lg border border-gray-200 text-gray-600 hover:border-gray-400 transition"
          >
            <Settings size={15} /> Edit
          </Link>
        )}
      </div>

      {/* Previous matches */}
      <section>
        <div className="flex items-center gap-2 mb-4">
          <Trophy size={18} className="text-gray-500" />
          <h2 className="text-base font-semibold text-gray-800">Previous matches</h2>
        </div>

        <MatchList matches={matches} loading={matchesLoading} backUsername={username ?? ""} />
      </section>
    </div>
  );
}

// --- Match history list ---

function MatchList({ matches, loading, backUsername }: { matches: MatchHistoryResponse[]; loading: boolean; backUsername: string }) {
  if (loading) {
    return <p className="text-sm text-gray-400 py-8 text-center">Loading matches…</p>;
  }

  if (matches.length === 0) {
    return (
      <p className="text-sm text-gray-400 py-8 text-center border border-dashed border-gray-200 rounded-xl">
        No matches played yet
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      {matches.map((match) => (
        <Link
          key={match.sessionId}
          to={`/scoreboard/${match.roomName}`}
          state={{ backTo: `/profile/${backUsername}`, backLabel: backUsername }}
          className="flex items-center gap-3 px-4 py-3 rounded-xl border border-gray-200 bg-white hover:border-gray-400 transition"
        >
          {/* Placement badge */}
          <div
            className={`w-10 h-10 shrink-0 rounded-full flex items-center justify-center text-sm font-semibold ${
              match.won ? "bg-amber-100 text-amber-700" : "bg-gray-100 text-gray-600"
            }`}
          >
            {match.won ? <Crown size={18} /> : `#${match.placement}`}
          </div>

          {/* Match info */}
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-900 truncate">{match.roomName}</p>
            <p className="text-xs text-gray-400">
              {match.game} · {match.players} players · {formatDate(match.playedAt)}
            </p>
          </div>

          {/* Score */}
          <div className="text-right shrink-0">
            <p className="text-sm font-semibold text-gray-900">{match.score}</p>
            <p className="text-xs text-gray-400">
              {match.won ? "Winner" : `${ordinal(match.placement)} place`}
            </p>
          </div>
        </Link>
      ))}
    </div>
  );
}

function formatDate(date: Date): string {
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function ordinal(n: number): string {
  const suffix = ["th", "st", "nd", "rd"][(n % 100 > 10 && n % 100 < 14) || n % 10 > 3 ? 0 : n % 10];
  return `${n}${suffix}`;
}
