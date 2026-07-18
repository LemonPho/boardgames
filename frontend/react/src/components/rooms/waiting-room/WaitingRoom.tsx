import { useRoomContext } from "../../../context/RoomContext";
import { useAlertsContext } from "../../../context/AlertsContext";
import { cancelRoom, leaveRoom } from "../../../api/rooms";
import { useNavigate } from "react-router-dom";
import PlayersList from "./PlayersList";
import AddPlayersModal from "./AddPlayersModal";
import InvitationsList from "./InvitationsList";
import { useSessionContext } from "../../../context/SessionContext";

const INVITE_PLAYERS_PANEL = "invite-players-room";

export default function WaitingRoom() {
  const { room, currentPlayer } = useRoomContext();
  const { handleCreateSession } = useSessionContext();

  const { setErrorMessage, setSuccessMessage } = useAlertsContext();

  const navigate = useNavigate();

  const handleStartGame = async (event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if(room == null) return;
    
    await handleCreateSession();
  }

  const handleCancelRoom = async (event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if (room == null) return;

    await cancelRoom(room.name, setErrorMessage);
    setSuccessMessage("Room cancelled");
    navigate("/");
  }

  const handleLeaveRoom = async (event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if(room == null) return;

    await leaveRoom(room.name, setErrorMessage);
    navigate("/");
  }

  if (!room) return null;

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 gap-6">

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
      <PlayersList currentPlayer={currentPlayer} INVITE_PLAYERS_PANEL={INVITE_PLAYERS_PANEL}/>
      <AddPlayersModal INVITE_PLAYERS_PANEL={INVITE_PLAYERS_PANEL}/>

      <InvitationsList currentPlayer={currentPlayer}/>

      {/* Actions */}
      <div className="w-full max-w-2xl flex gap-3">
        {currentPlayer?.role == "ADMIN" ? (
          <>
            <button 
              className="flex-1 bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-3 rounded-xl transition-colors"
              onClick={(event) => {handleStartGame(event)}}  
            >
              Start game
            </button>
            <button
              className="border border-red-200 text-red-500 hover:border-red-400 text-sm font-medium py-3 px-6 rounded-xl transition-colors"
              onClick={(event) => { handleCancelRoom(event) }}
            >
              Cancel room
            </button>
          </>
        ) : (
          <button 
            className="flex-1 border border-gray-200 hover:border-gray-400 text-gray-600 text-sm font-medium py-3 rounded-xl transition-colors"
            onClick={(event) => {handleLeaveRoom(event)}}
          >
            Leave room
          </button>
        )}
      </div>

    </div>
  );
}