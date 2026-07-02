package com.motomutterers.boardgames.rooms.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.user.model.User;

public interface RoomInvitationTokenRepository extends JpaRepository<RoomInvitationToken, UUID> {
    @Query("SELECT COUNT(ri) > 0 FROM RoomInvitationToken ri WHERE ri.user = :user AND ri.room = :room AND ri.status = :status AND ri.expiresAt > :now")
    boolean isUserAlreadyInvitedAndNotExpired(@Param("user") User user, @Param("room") Room room, @Param("status") InvitationStatus status, @Param("now") LocalDateTime now);
    
    @Query("SELECT ri.user FROM RoomInvitationToken ri WHERE ri.room = :room AND ri.user IN :users AND ri.status = :status AND ri.expiresAt > :now")
    Set<User> findUsersInvitedToRoom(@Param("users") List<User> users, @Param("room") Room room, @Param("status") InvitationStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT ri FROM RoomInvitationToken ri WHERE ri.room = :room")
    List<RoomInvitationToken> findRoomInvitations(@Param("room") Room room);

    List<RoomInvitationToken> findAllByRoomAndStatus(Room room, InvitationStatus status);

    Optional<RoomInvitationToken> findByToken(String token);

    Optional<RoomInvitationToken> findByRoomAndUser(Room room, User user);
}
