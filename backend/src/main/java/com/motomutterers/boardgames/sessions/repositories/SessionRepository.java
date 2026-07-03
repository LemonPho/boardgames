package com.motomutterers.boardgames.sessions.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.sessions.models.Session;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRoom(Room room);
}
