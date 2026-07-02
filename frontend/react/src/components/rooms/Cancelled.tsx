import { useNavigate } from "react-router-dom";
import { useRoomContext } from "../../context/RoomContext";

export default function Cancelled() {
  const { room } = useRoomContext();
  const navigate = useNavigate();

  console.log(room);

  if (!room) return null;

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col items-center justify-center p-6 gap-6">

      {/* Status card */}
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-8 flex flex-col items-center gap-4 text-center">
        <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center">
          <span className="text-red-400 text-xl">✕</span>
        </div>
        <div>
          <h1 className="text-lg font-semibold text-gray-800">{room.name}</h1>
          <p className="text-sm text-gray-400 mt-1">This room has been cancelled</p>
        </div>
        <span className="text-xs font-medium bg-red-50 text-red-400 px-3 py-1 rounded-full">
          Cancelled
        </span>
      </div>

      {/* Summary */}
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-6">
        <h2 className="text-sm font-semibold text-gray-700 mb-4">Room summary</h2>
        <div className="flex flex-col gap-3">
          <div className="flex justify-between text-sm">
            <span className="text-gray-400">Game</span>
            <span className="text-gray-800">{room.game.name}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-400">Players joined</span>
            <span className="text-gray-800">{room.players.length}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-400">Tracking</span>
            <span className="text-gray-800">{room.trackingMode === "SELF" ? "Self tracking" : "Admin tracking"}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-gray-400">Created</span>
            <span className="text-gray-800">{new Date(room.createdAt).toLocaleDateString()}</span>
          </div>
        </div>
      </div>

      {/* Action */}
      <div className="w-full max-w-md">
        <button
          onClick={() => navigate("/")}
          className="w-full bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-3 rounded-xl transition-colors"
        >
          Back to home
        </button>
      </div>

    </div>
  );
}