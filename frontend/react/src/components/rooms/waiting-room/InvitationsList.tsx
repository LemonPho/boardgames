import { revokeRoomInvite } from "../../../api/rooms";
import { useAlertsContext } from "../../../context/AlertsContext";
import { useRoomContext } from "../../../context/RoomContext";
import type { RoomUserResponse } from "../../../types/rooms";

export default function InvitationsList({ currentPlayer }: { currentPlayer: RoomUserResponse | null }) {
  const { room, setRoom } = useRoomContext();
  const { setErrorMessage } = useAlertsContext();

  if (room == null) return;
  if (room.invitations.length <= 0) return null

  const handleRevokeInvite = async(username: string, event: React.MouseEvent): Promise<void> => {
    event.stopPropagation();

    if(room == null) return;

    const response = await revokeRoomInvite(username, room.name, setErrorMessage);
    if (response) setRoom({ ...room, invitations: response });
  }

  return (

    <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
      <h2 className="text-sm font-semibold text-gray-700 mb-4">
        Pending invitations · {room.invitations.length}
      </h2>
      <div className="flex flex-col gap-2">
        {room.invitations.map((invitation) => (
          <div
            key={invitation.username}
            className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3"
          >
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-sm font-medium text-gray-600">
                {invitation.username[0].toUpperCase()}
              </div>
              <div>
                <p className="text-sm font-medium text-gray-800">{invitation.username}</p>
                <p className="text-xs text-gray-400">Invitation pending</p>
              </div>
            </div>
            {currentPlayer?.role === "ADMIN" && (
              <button
                className="text-xs text-red-400 hover:text-red-600 transition-colors"
                onClick={(event) => { handleRevokeInvite(invitation.username, event) }}
              >
                Revoke
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}