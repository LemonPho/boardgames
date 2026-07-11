import { useEffect, useState } from "react";
import Modal from "../../util/Modal";
import type { UserAvailabilityResponse } from "../../../types/user";
import UserSearchResult from "./UserSearchResult";
import { useRoomContext } from "../../../context/RoomContext";
import { useAlertsContext } from "../../../context/AlertsContext";
import { createAnonymousPlayer, invitePlayerToRoom, removePlayer, searchUsersAvailability } from "../../../api/rooms";
import type { RoomUserResponse } from "../../../types/rooms";
import { useUserContext } from "../../../context/UserContext";

export default function AddPlayersModal({ INVITE_PLAYERS_PANEL }: { INVITE_PLAYERS_PANEL: string}) {
  const { room } = useRoomContext();
  const { setSuccessMessage, setErrorMessage } = useAlertsContext();
  const { user } = useUserContext();

  const [inviteTab, setInviteTab] = useState<"search" | "anonymous">("search");
  const [usernameInput, setUsernameInput] = useState<string>("");
  const [usernameMatches, setUsernameMatches] = useState<UserAvailabilityResponse[]>([]);
  const [displayNameInput, setDisplayNameInput] = useState<string>("");
  const [anonymousPlayers, setAnonymousPlayers] = useState<RoomUserResponse[]>([]);
 
  const handleUsernameInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setUsernameInput(event.target.value);
  }

  const handleInviteUserToRoom = async (username: string, event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if (room == null) return;

    await invitePlayerToRoom(username, room.name, setErrorMessage);
    setSuccessMessage("User invited");
    await fetchUsernameMatches();
  }

  const handleDisplayNameInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setDisplayNameInput(event.target.value);
  }

  const handleCreateAnonymousPlayer = async (event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if (room == null) return;

    await createAnonymousPlayer(displayNameInput, room.name, setErrorMessage);
  }

  const handleRemovePlayer = async (roomUserId: string, event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if (room == null) return;

    await removePlayer(roomUserId, room.name, setErrorMessage);
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

  useEffect(() => {
    const timeout = setTimeout(async () => {
      await fetchUsernameMatches();
    }, 500);

    return () => clearTimeout(timeout);
  }, [usernameInput]);

  useEffect(() => {
    if (!room || !user) return;

    const anonymous = room.players.filter((p) => p.role === "ANONYMOUS");
    setAnonymousPlayers(anonymous);
  }, [room, user]);


  return (
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
                <UserSearchResult user={match} handleInviteUserToRoom={handleInviteUserToRoom} />
              ))}
            </div>
          )}
        </div>
      )}

      {inviteTab === "anonymous" && (
        <div className="flex flex-col gap-3">
          {/* Input row */}
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Enter display name..."
              className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
              onChange={handleDisplayNameInputChange}
            />
            <button
              onClick={(event) => handleCreateAnonymousPlayer(event)}
              className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium px-4 rounded-lg transition-colors"
            >
              Add
            </button>
          </div>

          {/* Anonymous players list */}
          {anonymousPlayers.length > 0 && (
            <div className="flex flex-col gap-1">
              {anonymousPlayers.map((player) => (
                <div
                  key={player.id}
                  className="flex items-center justify-between px-3 py-2 rounded-xl border border-gray-100"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-600">
                      {player.displayName?.[0]?.toUpperCase() ?? "?"}
                    </div>
                    <span className="text-sm text-gray-800">{player.displayName}</span>
                  </div>
                  <button
                    onClick={(event) => handleRemovePlayer(player.id, event)}
                    className="text-xs text-red-400 hover:text-red-600 transition-colors"
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </Modal>

  );
}