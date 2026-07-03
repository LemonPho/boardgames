package com.motomutterers.boardgames.sessions.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.CreateSessionRequest;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.events.SessionUpdatedEvent;
import com.motomutterers.boardgames.sessions.models.Session;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final SessionUtilitysService sessionUtilitysService;
    private final RoomsUtilityService roomsUtilityService;
    private final UserService userService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    public SessionService(
        SessionRepository sessionRepository,
        SessionUtilitysService sessionUtilitysService,
        RoomsUtilityService roomsUtilityService,
        UserService userService
    ) {
        this.sessionRepository = sessionRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.userService = userService;
        this.sessionUtilitysService = sessionUtilitysService;
    }

    public SessionResponse createSession(CreateSessionRequest request, Authentication authentication){
        String roomName = request.getRoomName();

        logger.info("Creating session for room " + roomName + " from user " + authentication.getName());
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        sessionUtilitysService.throwIfSessionExistsInRoom(room);

        Session session = new Session(room);
        sessionRepository.save(session);

        return new SessionResponse(session);
    }
}
