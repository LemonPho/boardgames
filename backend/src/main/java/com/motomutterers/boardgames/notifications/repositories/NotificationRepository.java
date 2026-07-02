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
    List<Notification> findNotificationsByUser(User user);

    @Modifying
    @Query(value = "DELETE FROM notifications WHERE payload->'room'->>'name' = :roomName AND type = 'ROOM_INVITATION'", nativeQuery=true)
    void deleteByRoomName(@Param("roomName") String roomName);
}
