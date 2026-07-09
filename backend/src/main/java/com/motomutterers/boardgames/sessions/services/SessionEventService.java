package com.motomutterers.boardgames.sessions.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.sessionevent.CreateRoundStartRequest;
import com.motomutterers.boardgames.sessions.events.SessionEventUpdatedEvent;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.repositories.SessionEventRepository;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SessionEventService {
    private final SessionEventRepository sessionEventRepository;
    private final UserService userService;
    private final RoomsUtilityService roomsUtilityService;
    private final SessionUtilitysService sessionUtilitysService;

    private final ObjectMapper objectMapper;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    public SessionEventService(
        SessionEventRepository sessionEventRepository,
        UserService userService,
        RoomsUtilityService roomsUtilityService,
        SessionUtilitysService sessionUtilitysService,
        ObjectMapper objectMapper
    ) {
        this.sessionEventRepository = sessionEventRepository;
        this.userService = userService;
        this.roomsUtilityService = roomsUtilityService;
        this.sessionUtilitysService = sessionUtilitysService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createRoundStart(CreateRoundStartRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(request.getRoomName());

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEventPayload payload = new SessionEventPayload.Bids(request.getRound(), request.getCardCount());

        SessionEvent sessionEvent = new SessionEvent(session, request.getType(), request.getSequence(), objectMapper.writeValueAsString(payload));
        sessionEventRepository.save(sessionEvent);

        roomsUtilityService.updateRoomLastUpdated(room);
        eventPublisher.publishEvent(new SessionEventUpdatedEvent(request.getRoomName(), sessionEvent));
    }
}
