package com.motomutterers.boardgames.sessions.services;

import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.teams.models.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamSessionEventServiceTest {

    @Mock private TeamSessionEventRepository teamSessionEventRepository;
    @Mock private RoomsUtilityService roomsUtilityService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TeamSessionEventService teamSessionEventService;

    @BeforeEach
    void setUp() {
        teamSessionEventService = new TeamSessionEventService(
            teamSessionEventRepository, roomsUtilityService, objectMapper);
    }

    // helpers
    private Session session() {
        Game game = new Game();
        game.setName("Skull King");
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        return new Session(room);
    }

    // --- upsertTeamSessionEvent ---

    @Test
    void upsert_noExistingEvent_insertsWithNextSequence() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = new Team(session);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());
        when(teamSessionEventRepository.countBySession(session)).thenReturn(3);

        TeamSessionEventPayload payload = new TeamSessionEventPayload.Bids(2);
        TeamSessionEvent result = teamSessionEventService.upsertTeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BIDS, payload);

        // A new row keyed on countBySession + 1.
        assertEquals(4, result.getSequence());
        assertEquals(team, result.getTeam());
        assertTrue(result.getPayload().contains("\"bid\":2"));
        verify(teamSessionEventRepository).save(result);
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void upsert_existingEvent_overwritesPayloadInPlace() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = new Team(session);
        TeamSessionEvent existing = new TeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BIDS, 7, "{\"bid\":1}");

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        TeamSessionEventPayload payload = new TeamSessionEventPayload.Bids(5);
        TeamSessionEvent result = teamSessionEventService.upsertTeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BIDS, payload);

        // Same row, corrected payload, sequence untouched. No new sequence lookup.
        assertSame(existing, result);
        assertEquals(7, result.getSequence());
        assertTrue(result.getPayload().contains("\"bid\":5"));
        verify(teamSessionEventRepository, never()).countBySession(any());
        verify(teamSessionEventRepository).save(existing);
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void upsert_serializesBonusPayload() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BONUS_POINTS, 0, "{}");
        Team team = new Team(session);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());
        when(teamSessionEventRepository.countBySession(session)).thenReturn(0);

        TeamSessionEventPayload payload = new TeamSessionEventPayload.BonusPoints(
            2, true, 1, 0, false, 4);
        TeamSessionEvent result = teamSessionEventService.upsertTeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BONUS_POINTS, payload);

        assertTrue(result.getPayload().contains("\"standardFourteens\":2"));
        assertTrue(result.getPayload().contains("\"blackFourteen\":true"));
        assertTrue(result.getPayload().contains("\"loot\":4"));
    }
}
