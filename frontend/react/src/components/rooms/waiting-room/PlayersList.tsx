import { useState } from "react";
import { ChevronUp, ChevronDown } from "lucide-react";
import { movePlayer, removePlayer, revokeRoomInvite } from "../../../api/rooms";
import { useAlertsContext } from "../../../context/AlertsContext";
import { useRoomContext } from "../../../context/RoomContext";
import { useUIContext } from "../../../context/UIContext";
import { useUserContext } from "../../../context/UserContext";
import type { RoomUserResponse } from "../../../types/rooms";
import SubmitButton from "../../util/SubmitButton";

export default function PlayersList({ currentPlayer, INVITE_PLAYERS_PANEL }: { currentPlayer: RoomUserResponse | null, INVITE_PLAYERS_PANEL: string }) {
  const { room } = useRoomContext();
  const { togglePanel } = useUIContext();
  const { user } = useUserContext();

  if(room == null) return;

  const isAdmin = currentPlayer?.role == "ADMIN";

  // Players arrive ordered by playing position (active players + pending invites;
  // declined ones are hidden by the backend).
  const players = room.players;

  return (
    <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-gray-700">
          Players · {players.length}/{room.game.maxPlayers}
        </h2>
        {isAdmin && (
          <div className="relative" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => togglePanel(INVITE_PLAYERS_PANEL)}
              className="text-sm text-gray-500 border border-gray-200 rounded-lg px-3 py-1.5 hover:border-gray-400 transition-colors"
            >
              + Add players
            </button>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-2">
        {players.map((player, index) => (
          <PlayerRow
            key={player.id}
            player={player}
            roomName={room.name}
            isAdmin={isAdmin}
            isSelf={player.user?.username === user?.username}
            index={index}
            total={players.length}
          />
        ))}
      </div>
    </div>
  );
}

// Each row owns its own loading state, so acting on one player only shows loading
// on that row's buttons.
function PlayerRow({
  player, roomName, isAdmin, isSelf, index, total,
}: {
  player: RoomUserResponse;
  roomName: string;
  isAdmin: boolean;
  isSelf: boolean;
  index: number;
  total: number;
}) {
  const { setErrorMessage } = useAlertsContext();
  const [loading, setLoading] = useState(false);

  const isPending = player.status === "PENDING_INVITE";

  const handleRemove = async (): Promise<void> => {
    await removePlayer(player.id, roomName, setErrorMessage);
  };

  // Revoking a pending invite is keyed by the invited user's username.
  const handleRevoke = async (): Promise<void> => {
    if (player.user?.username) await revokeRoomInvite(player.user.username, roomName, setErrorMessage);
  };

  const handleMove = async (newLocation: number): Promise<void> => {
    await movePlayer(player.id, newLocation, roomName, setErrorMessage);
  };

  return (
    <div className={`flex items-center justify-between border rounded-xl px-4 py-3 ${isPending ? "border-gray-100 bg-gray-50/60" : "border-gray-100"}`}>
      <div className="flex items-center gap-2 sm:gap-3">
        {/* Reorder controls (admin only). Flush to the row's top/left/bottom edges
            (negative margins cancel the row padding) and split the full row height,
            so the tap targets stay tall without enlarging the row. */}
        {isAdmin && total > 1 && (
          <div className="flex flex-col self-stretch -my-3 -ml-4 mr-1">
            <button
              onClick={() => handleMove(index - 1)}
              disabled={index === 0 || loading}
              aria-label="Move player up"
              className="flex flex-1 w-9 items-center justify-center rounded-tl-xl text-gray-400 hover:bg-gray-100 hover:text-gray-700 active:bg-gray-200 transition-colors disabled:opacity-25 disabled:hover:bg-transparent disabled:hover:text-gray-400 touch-manipulation"
            >
              <ChevronUp size={20} />
            </button>
            <button
              onClick={() => handleMove(index + 1)}
              disabled={index === total - 1 || loading}
              aria-label="Move player down"
              className="flex flex-1 w-9 items-center justify-center rounded-bl-xl text-gray-400 hover:bg-gray-100 hover:text-gray-700 active:bg-gray-200 transition-colors disabled:opacity-25 disabled:hover:bg-transparent disabled:hover:text-gray-400 touch-manipulation"
            >
              <ChevronDown size={20} />
            </button>
          </div>
        )}

        {/* Avatar */}
        <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-sm font-medium text-gray-600">
          {player?.role == "ANONYMOUS"
            ? (player.displayName?.[0] ?? "?").toUpperCase()
            : (player.user?.username?.[0] ?? "?").toUpperCase()
          }
        </div>

        {/* Name */}
        <div>
          <p className="text-sm font-medium text-gray-800">
            {player?.role == "ANONYMOUS" ? player.displayName : player.user?.username}
            {isSelf && <span className="ml-2 text-xs text-gray-400">(you)</span>}
          </p>
          {player?.role == "ANONYMOUS" && (
            <p className="text-xs text-gray-400">Anonymous</p>
          )}
          {isPending && (
            <p className="text-xs text-gray-400">Invitation pending</p>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2">
        {/* Role / status badge */}
        {player.role === "ADMIN" && (
          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
            Admin
          </span>
        )}
        {isPending && (
          <span className="text-xs bg-amber-100 text-amber-700 px-2 py-0.5 rounded-full">
            Invited
          </span>
        )}

        {/* Admin actions: revoke a pending invite, or kick a joined player (not self). */}
        {isAdmin && isPending && (
          <SubmitButton
            text="Revoke"
            loading={loading}
            setLoading={setLoading}
            onSubmit={handleRevoke}
            className="text-xs text-red-400 hover:text-red-600 transition-colors disabled:opacity-40"
          />
        )}
        {isAdmin && !isPending && !isSelf && (
          <SubmitButton
            text="Kick"
            loading={loading}
            setLoading={setLoading}
            onSubmit={handleRemove}
            className="text-xs text-red-400 hover:text-red-600 transition-colors disabled:opacity-40"
          />
        )}
      </div>
    </div>
  );
}
