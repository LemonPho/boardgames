import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Check, X, Gamepad2 } from "lucide-react";
import { useUIContext } from "../../context/UIContext";
import { useAlertsContext } from "../../context/AlertsContext";
import { useNotificationsContext } from "../../context/NotificationsContext";
import { markNotificationRead } from "../../api/notifications";
import { acceptInvite } from "../../api/rooms";
import type { NotificationResponse, RoomInvitationNotification } from "../../types/notifications";

export default function NotificationsPanel() {
  const { activePanel } = useUIContext();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const { notifications, removeNotification } = useNotificationsContext();
  const isOpen = activePanel === "notifications";

  if (!isOpen) return null;

  return (
    <div
      className="absolute right-0 mt-2 w-80 bg-white border border-gray-200 rounded-xl shadow-lg overflow-hidden z-50"
      onClick={(event) => {event.stopPropagation();}}
    >
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
        <h3 className="text-sm font-semibold text-gray-800">Notifications</h3>
      </div>

      <div className="max-h-96 overflow-y-auto divide-y divide-gray-50">
        {notifications.length === 0 && (
          <p className="px-4 py-6 text-center text-sm text-gray-400">No notifications</p>
        )}

        {notifications.map((notification) => (
          <NotificationRow
            key={notification.id}
            notification={notification}
            setErrorMessage={setErrorMessage}
            setSuccessMessage={setSuccessMessage}
            onRead={removeNotification}
          />
        ))}
      </div>
    </div>
  );
}

function NotificationRow({
  notification,
  setErrorMessage,
  setSuccessMessage,
  onRead,
}: {
  notification: NotificationResponse;
  setErrorMessage: (message: string) => void;
  setSuccessMessage: (message: string) => void;
  onRead: (id: string) => void;
}) {
  // Switch on the discriminator; `data` narrows to the matching payload.
  switch (notification.type) {
    case "ROOM_INVITATION":
      return (
        <RoomInvitationRow
          notification={notification}
          setErrorMessage={setErrorMessage}
          setSuccessMessage={setSuccessMessage}
          onRead={onRead}
        />
      );
    default:
      return null;
  }
}

function RoomInvitationRow({
  notification,
  setErrorMessage,
  setSuccessMessage,
  onRead,
}: {
  notification: RoomInvitationNotification;
  setErrorMessage: (message: string) => void;
  setSuccessMessage: (message: string) => void;
  onRead: (id: string) => void;
}) {
  const { roomName, gameName, roomAdminUsername, token } = notification.data;
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  const { closePanel } = useUIContext();

  const handleAccept = async (): Promise<void> => {
    setBusy(true);
    try {
      const room = await acceptInvite(token, setErrorMessage);
      // Mark read so it doesn't reappear, then remove it from the list.
      await markNotificationRead(notification.id, setErrorMessage);
      onRead(notification.id);
      setSuccessMessage(`Joined ${roomName}`);
      // Close the dropdown and drop the user straight into the room.
      closePanel();
      if (room) navigate(`/rooms/${room.name}`);
    } catch {
      /* surfaced via alerts */
    } finally {
      setBusy(false);
    }
  };

  const handleDecline = async (): Promise<void> => {
    setBusy(true);
    try {
      // No decline endpoint yet — dismiss by marking read so it disappears.
      await markNotificationRead(notification.id, setErrorMessage);
      onRead(notification.id);
    } catch {
      /* surfaced via alerts */
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      className={`flex gap-3 px-4 py-3 transition-colors hover:bg-gray-50 ${
        notification.read ? "" : "bg-blue-50/40"
      }`}
    >
      <div className="pt-1.5">
        <span
          className={`block w-2 h-2 rounded-full ${
            notification.read ? "bg-transparent" : "bg-blue-500"
          }`}
        />
      </div>

      <div className="w-9 h-9 shrink-0 rounded-full bg-gray-100 flex items-center justify-center">
        <Gamepad2 size={16} className="text-gray-500" />
      </div>

      <div className="flex-1 min-w-0">
        <p className="text-sm text-gray-700 leading-snug">
          <span className="font-semibold text-gray-900">{roomAdminUsername}</span>{" "}
          invited you to join{" "}
          <span className="font-medium text-gray-900">{roomName}</span>
        </p>
        <p className="text-xs text-gray-400 mt-0.5">{gameName}</p>

        <div className="flex gap-2 mt-2">
          <button
            onClick={handleAccept}
            disabled={busy}
            className="flex items-center gap-1 text-xs font-medium px-3 py-1.5 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition disabled:opacity-40"
          >
            <Check size={13} /> Accept
          </button>
          <button
            onClick={handleDecline}
            disabled={busy}
            className="flex items-center gap-1 text-xs font-medium px-3 py-1.5 rounded-lg border border-gray-200 text-gray-600 hover:border-gray-400 transition disabled:opacity-40"
          >
            <X size={13} /> Decline
          </button>
        </div>
      </div>
    </div>
  );
}
