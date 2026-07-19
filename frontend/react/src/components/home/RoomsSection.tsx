import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Check, X } from "lucide-react";
import type { RoomResponse } from "../../types/rooms";
import type { RoomInvitationNotification } from "../../types/notifications";
import { getActiveRoom, acceptInvite } from "../../api/rooms";
import { markNotificationRead } from "../../api/notifications";
import { useAlertsContext } from "../../context/AlertsContext";
import { useNotificationsContext } from "../../context/NotificationsContext";
import { useUserContext } from "../../context/UserContext";
import SubmitButton from "../util/SubmitButton";

import skullKingImage from "../../assets/skullking/skull-king-1-jeux-Toulon-L-Ataniere.webp";

/**
 * Home-page rooms section. Appears only when the user is currently in a room
 * or has a pending room invitation. The active room is fetched on load; invites
 * come live from the notifications context, so an incoming invite makes the
 * section appear without a refresh.
 *
 * Cards mirror the games grid (rounded image cards) so the home page reads as
 * one consistent surface, and use the same responsive grid for mobile.
 */
export default function RoomsSection() {
  const navigate = useNavigate();
  const { setErrorMessage } = useAlertsContext();
  const { notifications } = useNotificationsContext();
  const { user } = useUserContext();

  const [activeRoom, setActiveRoom] = useState<RoomResponse | null>(null);

  // Pending room invitations, surfaced live from notifications.
  const invites = notifications.filter(
    (n): n is RoomInvitationNotification => n.type === "ROOM_INVITATION"
  );

  useEffect(() => {
    // Only logged-in users have an active room; skip (and clear) otherwise.
    if (!user) {
      setActiveRoom(null);
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        const room = await getActiveRoom(setErrorMessage);
        if (!cancelled) setActiveRoom(room);
      } catch {
        /* surfaced via alerts */
      }
    })();
    return () => { cancelled = true; };
  }, [user]);

  // Nothing to show -> render nothing (section only appears when relevant).
  if (!activeRoom && invites.length === 0) return null;

  return (
    <div className="mb-10">
      <h2 className="text-xl sm:text-2xl font-medium text-gray-900 mb-1">Your rooms</h2>
      <p className="text-sm text-gray-500 mb-4">Rejoin your game or respond to an invite</p>

      <div className="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-3">
        {activeRoom && (
          <button
            onClick={() => navigate(`/rooms/${activeRoom.name}`)}
            className="relative rounded-2xl shadow-lg overflow-hidden aspect-[3/4] flex flex-col justify-end text-left hover:shadow-xl transition-shadow"
          >
            <img
              src={skullKingImage}
              alt={activeRoom.game.name}
              className="absolute inset-0 w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-black/40 backdrop-blur-[2px]" />
            <div className="relative z-10 p-4 flex flex-col gap-2">
              <span className="self-start text-[10px] font-semibold uppercase tracking-wide text-white/90 bg-white/20 rounded-full px-2 py-0.5">
                {activeRoom.status === "IN_PROGRESS" ? "In progress" : "Waiting"}
              </span>
              <div>
                <p className="text-base font-semibold text-white leading-tight line-clamp-2">
                  {activeRoom.name}
                </p>
                <p className="text-xs text-white/70 mt-0.5">{activeRoom.game.name}</p>
              </div>
              <span className="mt-1 w-full text-center text-sm font-medium py-2 rounded-lg bg-white text-gray-900">
                Rejoin →
              </span>
            </div>
          </button>
        )}

        {invites.map((invite) => (
          <InviteCard key={invite.id} invite={invite} />
        ))}
      </div>
    </div>
  );
}

// Each invite card owns its loading state, so accepting/declining one invite
// only disables that card's buttons — you can act on several invites at once,
// but a single invite can't be double-submitted.
function InviteCard({ invite }: { invite: RoomInvitationNotification }) {
  const navigate = useNavigate();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const { removeNotification } = useNotificationsContext();
  const [loading, setLoading] = useState(false);

  const handleAccept = async (): Promise<void> => {
    const room = await acceptInvite(invite.data.token, setErrorMessage);
    await markNotificationRead(invite.id, setErrorMessage);
    removeNotification(invite.id);
    setSuccessMessage(`Joined ${invite.data.roomName}`);
    if (room) navigate(`/rooms/${room.name}`);
  };

  const handleDecline = async (): Promise<void> => {
    await markNotificationRead(invite.id, setErrorMessage);
    removeNotification(invite.id);
  };

  return (
    <div className="relative rounded-2xl shadow-lg overflow-hidden aspect-[3/4] flex flex-col justify-end">
      <img
        src={skullKingImage}
        alt={invite.data.gameName}
        className="absolute inset-0 w-full h-full object-cover"
      />
      <div className="absolute inset-0 bg-black/40 backdrop-blur-[2px]" />
      <div className="relative z-10 p-4 flex flex-col gap-2">
        <span className="self-start text-[10px] font-semibold uppercase tracking-wide text-white/90 bg-white/20 rounded-full px-2 py-0.5">
          Invite
        </span>
        <div>
          <p className="text-sm text-white/80 leading-snug">
            <span className="font-semibold text-white">{invite.data.roomAdminUsername}</span> invited you to
          </p>
          <p className="text-base font-semibold text-white leading-tight line-clamp-2">
            {invite.data.roomName}
          </p>
          <p className="text-xs text-white/70 mt-0.5">{invite.data.gameName}</p>
        </div>
        <div className="flex gap-2 mt-1">
          <SubmitButton
            loading={loading}
            setLoading={setLoading}
            onSubmit={handleAccept}
            className="flex-1 flex items-center justify-center gap-1 text-sm font-medium py-2 rounded-lg bg-white text-gray-900 hover:bg-gray-100 transition active:scale-[0.98] disabled:opacity-40"
          >
            <Check size={15} /> Accept
          </SubmitButton>
          <SubmitButton
            loading={loading}
            setLoading={setLoading}
            onSubmit={handleDecline}
            className="flex items-center justify-center px-3 py-2 rounded-lg bg-white/15 text-white hover:bg-white/25 transition active:scale-[0.98] disabled:opacity-40"
          >
            <X size={16} />
          </SubmitButton>
        </div>
      </div>
    </div>
  );
}
