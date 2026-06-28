import { useEffect, useState } from "react";
import { useRoomContext } from "../../context/RoomContext";
import { useApplicationContext } from "../../context/ApplicationContext";
import type { RoomUserResponse } from "../../types/rooms";

export default function WaitingRoom() {
  const { room } = useRoomContext();
  const { user } = useApplicationContext();

  const [currentPlayer, setCurrentPlayer] = useState<RoomUserResponse | null>(null);

  useEffect(() => {
    if (!room || !user) return;

    const me = room.players.find((p) =>
      p.role != "ANONYMOUS" && p.user?.username === user.username
    );

    if (me) {
      setCurrentPlayer(me);
    }
  }, [room, user]);

  if (!room) return null;

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center p-6 gap-6">

      {/* Room header */}
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold text-gray-800">{room.name}</h1>
            <p className="text-sm text-gray-400 mt-1">{room.game.name} · {room.trackingMode === "SELF" ? "Self tracking" : "Admin tracking"}</p>
          </div>
          <span className="text-xs font-medium bg-yellow-100 text-yellow-700 px-3 py-1 rounded-full">
            Waiting
          </span>
        </div>
      </div>

      {/* Players */}
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-gray-700">
            Players · {room.players.length}/{room.game.maxPlayers}
          </h2>
          {currentPlayer?.role == "ADMIN" && (
            <button className="text-sm text-gray-500 border border-gray-200 rounded-lg px-3 py-1.5 hover:border-gray-400 transition-colors">
              + Add anonymous player
            </button>
          )}
        </div>

        <div className="flex flex-col gap-2">
          {room.players.map((player, index) => (
            <div
              key={index}
              className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3"
            >
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
                    {player.user?.username === user?.username && (
                      <span className="ml-2 text-xs text-gray-400">(you)</span>
                    )}
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
                {currentPlayer?.role == "ADMIN" && player.user?.username !== user?.username && (
                  <button className="text-xs text-red-400 hover:text-red-600 transition-colors">
                    Kick
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Actions */}
      <div className="w-full max-w-2xl flex gap-3">
        {currentPlayer?.role == "ADMIN" ? (
          <>
            <button className="flex-1 bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-3 rounded-xl transition-colors">
              Start game
            </button>
            <button className="border border-red-200 text-red-500 hover:border-red-400 text-sm font-medium py-3 px-6 rounded-xl transition-colors">
              Cancel room
            </button>
          </>
        ) : (
          <button className="flex-1 border border-gray-200 hover:border-gray-400 text-gray-600 text-sm font-medium py-3 rounded-xl transition-colors">
            Leave room
          </button>
        )}
      </div>

    </div>
  );
}