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
import com.motomutterers.boardgames.user.model.User;

public interface RoomUserRepository extends JpaRepository<RoomUser, UUID> {
    @Query("SELECT ru.user FROM RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u IN :users AND r.status IN :statuses")
    Set<User> findOccupiedUsers(@Param("users") List<User> users, @Param("statuses") List<RoomStatus> statuses);

    @Query("SELECT ru.room FROM RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u = :user AND r.status IN :statuses")
    Optional<Room> findActiveRoomByUser(@Param("user") User user, @Param("statuses") List<RoomStatus> statuses);

    @Query("SELECT COUNT(ru) > 0 FROM RoomUser ru JOIN ru.room r WHERE ru.user = :user AND r.status IN :activeStatuses")
    boolean isUserInActiveRoom(@Param("user") User user, @Param("activeStatuses") List<RoomStatus> activeStatuses);

    @Query("SELECT ru.room from RoomUser ru JOIN ru.user u JOIN ru.room r WHERE u IN :users AND r.status IN :statuses")
    Set<Room> findRoomsByUsersAndStatuses(@Param("users") List<User> users, @Param("statuses") List<RoomStatus> statuses);

    Optional<RoomUser> findByUserAndRoom(User user, Room room);
}
