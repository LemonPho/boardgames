package com.motomutterers.boardgames.sessions.services;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.rooms.model.Room.Room;
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
        Optional<Session> result = sessionRepository.findByRoom(room);
        if(result.isPresent()) throw new SessionExistsException("There is already a session for this room");
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void cancelSession(Session session){
        session.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(session);
    }

    public Session getOrThrowSessionByRoom(Room room){
        Optional<Session> result = sessionRepository.findByRoom(room);
        if(result.isEmpty()){
            throw new SessionNotFoundException("Session for room: " + room.getName() + " not found");
        }

        return result.get();
    }
}
