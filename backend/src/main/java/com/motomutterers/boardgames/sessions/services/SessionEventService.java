package com.motomutterers.boardgames.sessions.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.sessionevent.CreateRoundStartRequest;
import com.motomutterers.boardgames.sessions.dto.sessionevent.CreateSessionEventRequest;
import com.motomutterers.boardgames.sessions.events.SessionEventUpdatedEvent;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.repositories.SessionEventRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SessionEventService {
    private final SessionEventRepository sessionEventRepository;
    private final RoomsUtilityService roomsUtilityService;
    private final SessionUtilitysService sessionUtilitysService;

    private final ObjectMapper objectMapper;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    public SessionEventService(
        SessionEventRepository sessionEventRepository,
        RoomsUtilityService roomsUtilityService,
        SessionUtilitysService sessionUtilitysService,
        ObjectMapper objectMapper
    ) {
        this.sessionEventRepository = sessionEventRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.sessionUtilitysService = sessionUtilitysService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createRoundStart(CreateRoundStartRequest request){
        Room room = roomsUtilityService.getRoomByName(request.getRoomName());

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEventPayload payload = new SessionEventPayload.RoundStart(request.getRound(), request.getCardCount());

        SessionEvent sessionEvent = new SessionEvent(session, request.getType(), request.getSequence(), objectMapper.writeValueAsString(payload));
        sessionEventRepository.save(sessionEvent);

        eventPublisher.publishEvent(new SessionEventUpdatedEvent(request.getRoomName(), sessionEvent));
    }
}
