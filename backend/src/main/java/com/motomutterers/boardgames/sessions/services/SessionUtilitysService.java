package com.motomutterers.boardgames.sessions.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.sessions.exceptions.SessionExistsException;
import com.motomutterers.boardgames.sessions.models.Session;
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
}
