package com.motomutterers.boardgames.notifications.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.user.model.User;

public interface NotificationRepository extends JpaRepository<Notification, UUID>{
    // Only unread notifications are surfaced — reading one dismisses it.
    List<Notification> findNotificationsByUserAndReadFalse(User user);

    @Modifying
    @Query(value = "DELETE FROM notifications WHERE payload->>'roomName' = :roomName AND type = 'ROOM_INVITATION'", nativeQuery=true)
    void deleteByRoomName(@Param("roomName") String roomName);

    // The invitation notification(s) tied to a token — read as entities before
    // deleting so the caller can push a live removal per notification.
    @Query(value = "SELECT * FROM notifications WHERE payload->>'token' = :token AND type = 'ROOM_INVITATION'", nativeQuery=true)
    List<Notification> findByInvitationToken(@Param("token") String token);

    // Dismiss a room's invitations once it's over (completed/cancelled) — the
    // invite is no longer actionable, so it shouldn't linger as unread.
    @Modifying
    @Query(value = "UPDATE notifications SET read = true WHERE payload->>'roomName' = :roomName AND type = 'ROOM_INVITATION' AND read = false", nativeQuery=true)
    void markReadByRoomName(@Param("roomName") String roomName);

    // The unread invitation(s) tied to a token — the token uniquely identifies
    // one invite, so this leaves other invitees to the same room untouched. Read
    // as entities (not a bulk update) so the caller can push a live dismissal
    // per notification.
    @Query(value = "SELECT * FROM notifications WHERE payload->>'token' = :token AND type = 'ROOM_INVITATION' AND read = false", nativeQuery=true)
    List<Notification> findUnreadByInvitationToken(@Param("token") String token);
}
