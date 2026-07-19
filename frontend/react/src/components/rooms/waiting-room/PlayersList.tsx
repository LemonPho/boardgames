import { useState } from "react";
import { removePlayer } from "../../../api/rooms";
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

  return (
    <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-gray-700">
          Players · {room.players.length}/{room.game.maxPlayers}
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
        {room.players.map((player) => (
          <PlayerRow
            key={player.id}
            player={player}
            roomName={room.name}
            isAdmin={isAdmin}
            isSelf={player.user?.username === user?.username}
          />
        ))}
      </div>
    </div>
  );
}

// Each row owns its own loading state, so kicking one player only shows loading
// on that row's button.
function PlayerRow({
  player, roomName, isAdmin, isSelf,
}: {
  player: RoomUserResponse;
  roomName: string;
  isAdmin: boolean;
  isSelf: boolean;
}) {
  const { setErrorMessage } = useAlertsContext();
  const [loading, setLoading] = useState(false);

  const handleRemove = async (): Promise<void> => {
    await removePlayer(player.id, roomName, setErrorMessage);
  };

  return (
    <div className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3">
      <div className="flex items-center gap-3">
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
        </div>
      </div>

      <div className="flex items-center gap-2">
        {/* Role badge */}
        {player.role === "ADMIN" && (
          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
            Admin
          </span>
        )}

        {/* Kick button - admin only, can't kick yourself */}
        {isAdmin && !isSelf && (
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
