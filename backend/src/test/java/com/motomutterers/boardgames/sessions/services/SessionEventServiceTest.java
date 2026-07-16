package com.motomutterers.boardgames.sessions.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.repositories.SessionEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionEventServiceTest {

    @Mock private SessionEventRepository sessionEventRepository;
    @Mock private RoomsUtilityService roomsUtilityService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SessionEventService sessionEventService;

    @BeforeEach
    void setUp() {
        // Real ObjectMapper so JSON (de)serialization is exercised, not mocked.
        sessionEventService = new SessionEventService(sessionEventRepository, roomsUtilityService, objectMapper);
    }

    // helpers
    private Session session() {
        Game game = new Game();
        game.setName("Skull King");
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        return new Session(room);
    }

    // --- createSessionEvent ---

    @Test
    void createSessionEvent_firstEvent_startsAtSequenceZero() {
        Session session = session();
        when(sessionEventRepository.findTopBySessionOrderBySequenceDesc(session))
            .thenReturn(Optional.empty());

        SessionEventPayload payload = new SessionEventPayload.Bids(1, 1, "team-1");
        SessionEvent event = sessionEventService.createSessionEvent(session, SessionEventType.BIDS, payload);

        assertEquals(0, event.getSequence());
        assertEquals(SessionEventType.BIDS, event.getType());
        assertTrue(event.getPayload().contains("\"round\":1"));
        verify(sessionEventRepository).save(event);
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void createSessionEvent_existingEvents_incrementsSequence() {
        Session session = session();
        SessionEvent previous = new SessionEvent(session, SessionEventType.BIDS, 4, "{}");
        when(sessionEventRepository.findTopBySessionOrderBySequenceDesc(session))
            .thenReturn(Optional.of(previous));

        SessionEventPayload payload = new SessionEventPayload.InProgress(2, 2, "team-1");
        SessionEvent event = sessionEventService.createSessionEvent(session, SessionEventType.IN_PROGRESS, payload);

        assertEquals(5, event.getSequence());
    }

    @Test
    void createSessionEvent_serializesPayloadToJson() {
        Session session = session();
        when(sessionEventRepository.findTopBySessionOrderBySequenceDesc(session))
            .thenReturn(Optional.empty());

        SessionEventPayload payload = new SessionEventPayload.TrickResults(3, 3, "team-2", true);
        SessionEvent event = sessionEventService.createSessionEvent(session, SessionEventType.TRICK_RESULTS, payload);

        assertTrue(event.getPayload().contains("\"krakenPlayed\":true"));
        assertTrue(event.getPayload().contains("\"startingTeamId\":\"team-2\""));
    }

    // --- getOrThrowCurrentEvent ---

    @Test
    void getOrThrowCurrentEvent_exists_returns() {
        Session session = session();
        SessionEvent event = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        when(sessionEventRepository.findTopBySessionOrderBySequenceDesc(session))
            .thenReturn(Optional.of(event));

        assertEquals(event, sessionEventService.getOrThrowCurrentEvent(session));
    }

    @Test
    void getOrThrowCurrentEvent_none_throws() {
        Session session = session();
        when(sessionEventRepository.findTopBySessionOrderBySequenceDesc(session))
            .thenReturn(Optional.empty());

        assertThrows(BadActionException.class, () -> sessionEventService.getOrThrowCurrentEvent(session));
    }

    // --- findLatestEventOfType ---

    @Test
    void findLatestEventOfType_delegatesToRepository() {
        Session session = session();
        SessionEvent event = new SessionEvent(session, SessionEventType.TRICK_RESULTS, 2, "{}");
        when(sessionEventRepository.findTopBySessionAndTypeOrderBySequenceDesc(session, SessionEventType.TRICK_RESULTS))
            .thenReturn(Optional.of(event));

        assertEquals(Optional.of(event),
            sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS));
    }

    // --- findAllEvents ---

    @Test
    void findAllEvents_delegatesToRepository() {
        Session session = session();
        List<SessionEvent> events = List.of(new SessionEvent(session, SessionEventType.BIDS, 0, "{}"));
        when(sessionEventRepository.findBySessionOrderBySequenceAsc(session)).thenReturn(events);

        assertEquals(events, sessionEventService.findAllEvents(session));
    }

    // --- updatePayload ---

    @Test
    void updatePayload_overwritesPayloadAndSaves() {
        Session session = session();
        SessionEvent event = new SessionEvent(session, SessionEventType.BIDS, 0, "{\"round\":1}");

        SessionEventPayload payload = new SessionEventPayload.Bids(1, 5, "team-9");
        sessionEventService.updatePayload(event, payload);

        assertTrue(event.getPayload().contains("\"cardCount\":5"));
        assertTrue(event.getPayload().contains("\"startingTeamId\":\"team-9\""));
        verify(sessionEventRepository).save(event);
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }
}
