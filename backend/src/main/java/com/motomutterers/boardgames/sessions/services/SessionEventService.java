package com.motomutterers.boardgames.sessions.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.repositories.SessionEventRepository;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SessionEventService {
    private final SessionEventRepository sessionEventRepository;
    private final TeamSessionEventRepository teamSessionEventRepository;
    private final RoomsUtilityService roomsUtilityService;
    private final ObjectMapper objectMapper;

    public SessionEventService(
        SessionEventRepository sessionEventRepository,
        TeamSessionEventRepository teamSessionEventRepository,
        RoomsUtilityService roomsUtilityService,
        ObjectMapper objectMapper
    ) {
        this.sessionEventRepository = sessionEventRepository;
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionEvent createSessionEvent(Session session, SessionEventType type, SessionEventPayload payload){
        int nextSequence = sessionEventRepository.findTopBySessionOrderBySequenceDesc(session)
            .map(event -> event.getSequence() + 1)
            .orElse(0);

        SessionEvent sessionEvent = new SessionEvent(session, type, nextSequence, objectMapper.writeValueAsString(payload));
        sessionEventRepository.save(sessionEvent);

        roomsUtilityService.updateRoomLastUpdated(session.getRoom());

        return sessionEvent;
    }

    public SessionEvent getOrThrowCurrentEvent(Session session){
        return sessionEventRepository.findTopBySessionOrderBySequenceDesc(session)
            .orElseThrow(() -> new BadActionException("Session has no events"));
    }

    public Optional<SessionEvent> findLatestEventOfType(Session session, SessionEventType type){
        return sessionEventRepository.findTopBySessionAndTypeOrderBySequenceDesc(session, type);
    }

    public List<SessionEvent> findAllEvents(Session session){
        return sessionEventRepository.findBySessionOrderBySequenceAsc(session);
    }

    @Transactional
    public void updatePayload(SessionEvent sessionEvent, SessionEventPayload payload){
        sessionEvent.setPayload(objectMapper.writeValueAsString(payload));
        sessionEventRepository.save(sessionEvent);
        roomsUtilityService.updateRoomLastUpdated(sessionEvent.getSession().getRoom());
    }

    // Deletes these phase events and their per-team rows. Team rows are removed explicitly
    // (not only via the DB cascade) so the persistence context doesn't hold stale entities.
    // Flushes so a following createSessionEvent's max(sequence) query sees the deletions.
    @Transactional
    public void deleteEvents(List<SessionEvent> events){
        for(SessionEvent event : events){
            teamSessionEventRepository.deleteBySessionEvent(event);
        }
        sessionEventRepository.deleteAll(events);
        sessionEventRepository.flush();
    }
}
