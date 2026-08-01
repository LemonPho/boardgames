// Discriminated notification envelope mirroring the backend NotificationResponse.
// The client switches on `type` to narrow `data` to the matching payload — no
// parsing, no ad-hoc key access.

export type NotificationType = "ROOM_INVITATION";

export interface RoomInvitationData {
  roomName: string;
  gameName: string;
  roomAdminUsername: string;
  token: string;
}

interface BaseNotification<T extends NotificationType, D> {
  id: string;
  type: T;
  read: boolean;
  createdAt: string;
  data: D;
}

export type RoomInvitationNotification = BaseNotification<"ROOM_INVITATION", RoomInvitationData>;

// Union of every notification shape. Add new members as new types are introduced.
export type NotificationResponse = RoomInvitationNotification;

// Envelope pushed on the user's notification topic. `CREATED` carries a new
// notification to add; `READ` carries the id of one to dismiss (remove locally).
export type NotificationSocketMessage =
  | { event: "CREATED"; notification: NotificationResponse }
  | { event: "READ"; id: string };
