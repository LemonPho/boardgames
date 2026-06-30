import { useEffect, useState } from "react";
import { useRoomContext } from "../../../context/RoomContext";
import { useUserContext } from "../../../context/UserContext";
import type { InvitationErrorResponse, RoomUserResponse } from "../../../types/rooms";
import { useUIContext } from "../../../context/UIContext";
import { useAlertsContext } from "../../../context/AlertsContext";
import Modal from "../../util/Modal";
import type { UserAvailabilityResponse } from "../../../types/user";
import { invitePlayerToRoom, searchUsersAvailability } from "../../../api/rooms";
import UserSearchResult from "./UserSearchResult";

const INVITE_PLAYERS_PANEL = "invite-players-room";

export default function WaitingRoom() {
  const { room } = useRoomContext();
  const { user } = useUserContext();

  const { togglePanel } = useUIContext();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();

  const [currentPlayer, setCurrentPlayer] = useState<RoomUserResponse | null>(null);
  const [usernameInput, setUsernameInput] = useState<string>("");
  const [inviteTab, setInviteTab] = useState<"search" | "anonymous">("search");
  const [usernameMatches, setUsernameMatches] = useState<UserAvailabilityResponse[]>([]);

  const [errors, setErrors] = useState<InvitationErrorResponse | null>(null);

  const handleUsernameInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setUsernameInput(event.target.value);
  }

  const fetchUsernameMatches = async (): Promise<void> => {
    if (usernameInput.trim() === "") {
      setUsernameMatches([]);
      return;
    }

    if (room == undefined || room == null) return;

    const response = await searchUsersAvailability(usernameInput, room.name, setErrorMessage);
    if (response) setUsernameMatches(response);
    
  }

  const handleInviteUserToRoom = async (username: string, event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();
    
    if(room == null) return;
    
    await invitePlayerToRoom(username, room.name, setErrors, setErrorMessage);
    setSuccessMessage("User invited");
    setUsernameMatches(prev => 
      prev.map(match => 
        match.username === username 
          ? { ...match, invited: true } 
          : match
      )
    );
  }

  useEffect(() => {
    const timeout = setTimeout(async () => {
      await fetchUsernameMatches();
    }, 500);

    return () => clearTimeout(timeout);
  }, [usernameInput]);

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
            <>
              <div className="relative" onClick={(e) => e.stopPropagation()}>
                <button
                  onClick={() => togglePanel(INVITE_PLAYERS_PANEL)}
                  className="text-sm text-gray-500 border border-gray-200 rounded-lg px-3 py-1.5 hover:border-gray-400 transition-colors"
                >
                  + Add players
                </button>
              </div>

              <Modal id={INVITE_PLAYERS_PANEL} title="Add players">
                {/* Tabs */}
                <div className="flex gap-2 mb-4">
                  <button
                    onClick={() => setInviteTab("search")}
                    className={`text-sm px-3 py-1.5 rounded-lg transition-colors ${inviteTab === "search"
                      ? "bg-gray-800 text-white"
                      : "text-gray-500 border border-gray-200 hover:border-gray-400"
                      }`}
                  >
                    Find player
                  </button>
                  <button
                    onClick={() => setInviteTab("anonymous")}
                    className={`text-sm px-3 py-1.5 rounded-lg transition-colors ${inviteTab === "anonymous"
                      ? "bg-gray-800 text-white"
                      : "text-gray-500 border border-gray-200 hover:border-gray-400"
                      }`}
                  >
                    Add anonymous
                  </button>
                </div>

                {inviteTab === "search" && (
                  <div className="flex flex-col gap-2">
                    <input
                      type="text"
                      placeholder="Search by username..."
                      value={usernameInput}
                      onChange={handleUsernameInputChange}
                      className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
                    />
                    {usernameMatches.length > 0 && (
                      <div className="flex flex-col gap-1 mt-1">
                        {usernameMatches.map((match) => (
                          <UserSearchResult user={match} handleInviteUserToRoom={handleInviteUserToRoom}/>
                          ))}
                      </div>
                    )}
                  </div>
                )}

                {inviteTab === "anonymous" && (
                  <input
                    type="text"
                    placeholder="Enter display name..."
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
                  />
                  // submit button will go here
                )}
              </Modal>

            </>
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