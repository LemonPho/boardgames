import { useState } from "react";
import { revokeRoomInvite } from "../../../api/rooms";
import { useAlertsContext } from "../../../context/AlertsContext";
import { useRoomContext } from "../../../context/RoomContext";
import type { RoomInvitationResponse, RoomUserResponse } from "../../../types/rooms";
import SubmitButton from "../../util/SubmitButton";

export default function InvitationsList({ currentPlayer }: { currentPlayer: RoomUserResponse | null }) {
  const { room } = useRoomContext();

  if (room == null) return;
  if (room.invitations.length <= 0) return null;

  const isAdmin = currentPlayer?.role === "ADMIN";

  return (
    <div className="bg-white rounded-2xl shadow-lg w-full max-w-2xl p-6">
      <h2 className="text-sm font-semibold text-gray-700 mb-4">
        Pending invitations · {room.invitations.length}
      </h2>
      <div className="flex flex-col gap-2">
        {room.invitations.map((invitation) => (
          <InvitationRow
            key={invitation.username}
            invitation={invitation}
            roomName={room.name}
            isAdmin={isAdmin}
          />
        ))}
      </div>
    </div>
  );
}

// Each row owns its loading state, so revoking one invite only loads that row.
function InvitationRow({
  invitation, roomName, isAdmin,
}: {
  invitation: RoomInvitationResponse;
  roomName: string;
  isAdmin: boolean;
}) {
  const { setErrorMessage } = useAlertsContext();
  const [loading, setLoading] = useState(false);

  const handleRevoke = async (): Promise<void> => {
    await revokeRoomInvite(invitation.username, roomName, setErrorMessage);
  };

  return (
    <div className="flex items-center justify-between border border-gray-100 rounded-xl px-4 py-3">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center text-sm font-medium text-gray-600">
          {invitation.username[0].toUpperCase()}
        </div>
        <div>
          <p className="text-sm font-medium text-gray-800">{invitation.username}</p>
          <p className="text-xs text-gray-400">Invitation pending</p>
        </div>
      </div>
      {isAdmin && (
        <SubmitButton
          text="Revoke"
          loading={loading}
          setLoading={setLoading}
          onSubmit={handleRevoke}
          className="text-xs text-red-400 hover:text-red-600 transition-colors disabled:opacity-40"
        />
      )}
    </div>
  );
}
