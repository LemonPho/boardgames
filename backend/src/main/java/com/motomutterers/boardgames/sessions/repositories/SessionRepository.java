package com.motomutterers.boardgames.sessions.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;

import jakarta.persistence.LockModeType;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByRoom(Room room);
    Optional<Session> findByRoomAndStatus(Room room, SessionStatus status);

    // SELECT ... FOR UPDATE on the session row, held until the transaction ends. Serialises
    // every team-value write within one session (see TeamSessionEventService).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Session s where s.id = :id")
    Optional<Session> lockById(@Param("id") UUID id);
}
