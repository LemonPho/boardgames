package com.motomutterers.boardgames.rooms.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.rooms.model.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findById(UUID id);
    Optional<Room> findByName(String name);
}
