import { useEffect, useRef, useState } from "react";
import Modal from "../../util/Modal";
import type { UserAvailabilityResponse } from "../../../types/user";
import UserSearchResult from "./UserSearchResult";
import { useRoomContext } from "../../../context/RoomContext";
import { useAlertsContext } from "../../../context/AlertsContext";
import { createAnonymousPlayer, invitePlayerToRoom, removePlayer, searchUsersAvailability } from "../../../api/rooms";
import type { RoomUserResponse } from "../../../types/rooms";
import { useUserContext } from "../../../context/UserContext";
import SubmitButton from "../../util/SubmitButton";

const SEARCH_DEBOUNCE_MS = 500;

// Matches are stored with the term they were fetched for, so the UI can tell
// "no results yet for what you just typed" from "this term really has no matches"
// — otherwise the empty state flashes during the debounce and the request.
interface SearchResults {
  term: string;
  matches: UserAvailabilityResponse[];
}

const NO_RESULTS: SearchResults = { term: "", matches: [] };

export default function AddPlayersModal({ INVITE_PLAYERS_PANEL }: { INVITE_PLAYERS_PANEL: string}) {
  const { room } = useRoomContext();
  const { setSuccessMessage, setErrorMessage } = useAlertsContext();
  const { user } = useUserContext();

  const [inviteTab, setInviteTab] = useState<"search" | "anonymous">("search");
  const [usernameInput, setUsernameInput] = useState<string>("");
  const [results, setResults] = useState<SearchResults>(NO_RESULTS);
  const [searching, setSearching] = useState<boolean>(false);
  const [displayNameInput, setDisplayNameInput] = useState<string>("");
  const [anonymousPlayers, setAnonymousPlayers] = useState<RoomUserResponse[]>([]);
  const [loading, setLoading] = useState(false);

  const query = usernameInput.trim();
  // The term the user is currently on, so out-of-order responses can be discarded.
  const latestTerm = useRef<string>("");

  // Pending invitations reserve a seat and now live in the players list (declined
  // ones are excluded by the backend), so occupancy is just the players count.
  // Mirrors the backend capacity rule.
  const maxPlayers = room?.game.maxPlayers ?? 0;
  const occupied = room?.players.length ?? 0;
  const isFull = maxPlayers > 0 && occupied >= maxPlayers;

  const handleUsernameInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setUsernameInput(event.target.value);
  }

  const handleInviteUserToRoom = async (username: string): Promise<void> => {
    if (room == null) return;
    if (isFull) {
      setErrorMessage("Room is full");
      return;
    }

    await invitePlayerToRoom(username, room.name, setErrorMessage);
    setSuccessMessage("User invited");
    // Re-run the current search so the invited user's row flips to "Invited".
    await runSearch(query);
  }

  const handleDisplayNameInputChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setDisplayNameInput(event.target.value);
  }

  const handleCreateAnonymousPlayer = async (): Promise<void> => {
    if (room == null) return;
    if (isFull) {
      setErrorMessage("Room is full");
      return;
    }

    await createAnonymousPlayer(displayNameInput, room.name, setErrorMessage);
  }

  const runSearch = async (term: string): Promise<void> => {
    if (room == null || term === "") return;

    try {
      const response = await searchUsersAvailability(term, room.name, setErrorMessage);
      // Ignore a response the user has already typed past, so a slow request
      // can't overwrite the results of a newer one.
      if (latestTerm.current !== term) return;
      setResults({ term, matches: response ?? [] });
    } finally {
      if (latestTerm.current === term) setSearching(false);
    }
  }

  useEffect(() => {
    latestTerm.current = query;

    if (query === "") {
      setResults(NO_RESULTS);
      setSearching(false);
      return;
    }

    // Marked as searching for the whole debounce window too: until the results for
    // this exact term arrive, we don't know yet whether there are any matches.
    setSearching(true);
    // The failure is already surfaced by setErrorMessage; catch so the rethrow from
    // the api layer doesn't become an unhandled rejection.
    const timeout = setTimeout(() => { runSearch(query).catch(() => {}); }, SEARCH_DEBOUNCE_MS);

    return () => clearTimeout(timeout);
  }, [query, room?.name]);

  useEffect(() => {
    if (!room || !user) return;

    const anonymous = room.players.filter((p) => p.role === "ANONYMOUS");
    setAnonymousPlayers(anonymous);
  }, [room, user]);


  // Results for an older term stay on screen while the next search runs, so the list
  // doesn't blank out on every keystroke. "No users found" waits for the results of
  // the term actually in the box — never shown while a search is still pending.
  const resultsAreCurrent = results.term === query;
  const showMatches = query !== "" && results.matches.length > 0;
  const showEmptyState = query !== "" && !searching && resultsAreCurrent && results.matches.length === 0;

  return (
    <Modal id={INVITE_PLAYERS_PANEL} title="Add players">
      {isFull && (
        <div className="mb-4 rounded-lg bg-amber-50 border border-amber-200 px-3 py-2 text-sm text-amber-700">
          Room is full ({occupied}/{maxPlayers}). Remove a player or cancel an invite to add more.
        </div>
      )}

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
            disabled={isFull}
            className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed"
          />
          {showMatches && (
            <div className="flex flex-col gap-1 mt-1">
              {results.matches.map((match) => (
                <UserSearchResult key={match.username} user={match} handleInviteUserToRoom={handleInviteUserToRoom} />
              ))}
            </div>
          )}
          {showEmptyState && (
            <div className="text-xs text-gray-400 bg-gray-100 px-2 py-1">No users found</div>
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
              disabled={isFull}
              className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed"
              onChange={handleDisplayNameInputChange}
            />
            <SubmitButton
              text="Add"
              loading={loading}
              setLoading={setLoading}
              onSubmit={handleCreateAnonymousPlayer}
              disabled={isFull}
              className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium px-4 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            />
          </div>

          {/* Anonymous players list */}
          {anonymousPlayers.length > 0 && room && (
            <div className="flex flex-col gap-1">
              {anonymousPlayers.map((player) => (
                <AnonymousPlayerRow key={player.id} player={player} roomName={room.name} />
              ))}
            </div>
          )}
        </div>
      )}
    </Modal>

  );
}

// Own loading state per row, so removing one anonymous player only loads its button.
function AnonymousPlayerRow({ player, roomName }: { player: RoomUserResponse; roomName: string }) {
  const { setErrorMessage } = useAlertsContext();
  const [loading, setLoading] = useState(false);

  const handleRemove = async (): Promise<void> => {
    await removePlayer(player.id, roomName, setErrorMessage);
  };

  return (
    <div className="flex items-center justify-between px-3 py-2 rounded-xl border border-gray-100">
      <div className="flex items-center gap-3">
        <div className="w-7 h-7 rounded-full bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-600">
          {player.displayName?.[0]?.toUpperCase() ?? "?"}
        </div>
        <span className="text-sm text-gray-800">{player.displayName}</span>
      </div>
      <SubmitButton
        text="Remove"
        loading={loading}
        setLoading={setLoading}
        onSubmit={handleRemove}
        className="text-xs text-red-400 hover:text-red-600 transition-colors disabled:opacity-40"
      />
    </div>
  );
}