package com.motomutterers.boardgames.rooms.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus;
import com.motomutterers.boardgames.user.model.User;

public interface RoomUserRepository extends JpaRepository<RoomUser, UUID> {
    // "Occupied" means actually playing (ACTIVE) — a PENDING_INVITE / DECLINED row
    // is not membership, so it must not mark a user as busy elsewhere.
    @Query("SELECT ru.user FROM RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u IN :users AND r.status IN :statuses AND ru.status = :userStatus")
    Set<User> findOccupiedUsers(@Param("users") List<User> users, @Param("statuses") List<RoomStatus> statuses, @Param("userStatus") RoomUserStatus userStatus);

    @Query("SELECT ru.room FROM RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u = :user AND r.status IN :statuses AND ru.status = :userStatus")
    Optional<Room> findActiveRoomByUser(@Param("user") User user, @Param("statuses") List<RoomStatus> statuses, @Param("userStatus") RoomUserStatus userStatus);

    @Query("SELECT ru.room from RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u IN :users AND r.status IN :statuses AND ru.status = :userStatus")
    Set<Room> findRoomsByUsersAndStatuses(@Param("users") List<User> users, @Param("statuses") List<RoomStatus> statuses, @Param("userStatus") RoomUserStatus userStatus);

    Optional<RoomUser> findByUserAndRoom(User user, Room room);

    // Highest seat currently held in a room (players + pending invites), so a new
    // occupant can take the next position. Null when the room has no rows yet.
    @Query("SELECT MAX(ru.playingPosition) FROM RoomUser ru WHERE ru.room = :room")
    Integer findMaxPlayingPosition(@Param("room") Room room);
}
