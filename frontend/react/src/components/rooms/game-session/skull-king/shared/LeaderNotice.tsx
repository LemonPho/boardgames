import type { TeamResponse } from "../../../../../types/teams";

// Tells a player who leads this round, when it isn't them (their own card already says
// "You go first"). Same amber as that text.
export function LeaderNotice({ teams, startingTeamId, myTeamId }: {
  teams: TeamResponse[]; startingTeamId: string | null | undefined; myTeamId: string | undefined;
}) {
  if (!startingTeamId || startingTeamId === myTeamId) return null;

  const name = teams.find((team) => team.id === startingTeamId)?.player.displayName;
  if (!name) return null;

  return (
    <p className="text-center mb-2 text-sm font-medium text-amber-600 dark:text-amber-500">▶ {name} goes first</p>
  );
}
