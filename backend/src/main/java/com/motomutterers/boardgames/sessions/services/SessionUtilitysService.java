package com.motomutterers.boardgames.sessions.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.sessions.exceptions.SessionExistsException;
import com.motomutterers.boardgames.sessions.exceptions.SessionNotFoundException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;

@Service
public class SessionUtilitysService {
    private final SessionRepository sessionRepository;

    public SessionUtilitysService(
        SessionRepository sessionRepository
    ) {
        this.sessionRepository = sessionRepository;
    }

    public void throwIfSessionExistsInRoom(Room room){
        Optional<Session> result = sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS);
        if(result.isPresent()) throw new SessionExistsException("There is already a session for this room");
    }

    public Session getOrThrowSessionById(String id){
        Optional<Session> result = sessionRepository.findById(UUID.fromString(id));
        if(result.isEmpty()) throw new SessionNotFoundException("Session with id: " + id + " not found");
        return result.get();
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void cancelSession(Session session){
        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);
    }

    public void completeSession(Session session){
        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    public void cancelSessionIfExists(Room room){
        Optional<Session> result = sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS);
        if(result.isPresent()){
            Session session = result.get();
            session.setStatus(SessionStatus.CANCELLED);
            sessionRepository.save(session);
        }
    }

    public Session getOrThrowSessionByRoom(Room room){
        Optional<Session> result = sessionRepository.findByRoomAndStatus(room, SessionStatus.IN_PROGRESS);
        if(result.isEmpty()){
            throw new SessionNotFoundException("Session for room: " + room.getName() + " not found");
        }

        return result.get();
    }

    // The room's session regardless of status (a room has one). Used to read a
    // completed session's final scoreboard, where the IN_PROGRESS lookup fails.
    public Session getOrThrowSessionByRoomAnyStatus(Room room){
        return sessionRepository.findByRoom(room)
            .orElseThrow(() -> new SessionNotFoundException("Session for room: " + room.getName() + " not found"));
    }

    public void throwIfUserIsntRoomAdmin(RoomUser roomUser){
        if(!roomUser.getRole().equals(RoomUserRoles.ADMIN)){
            throw new UnauthorizedException("You aren't authorized to do this action");
        }
    }
}
