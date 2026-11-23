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
import com.motomutterers.boardgames.exceptions.VersionConflictException;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.sessions.services.TeamSessionEventService.WriteMode;
import com.motomutterers.boardgames.teams.models.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamSessionEventServiceTest {

    @Mock private TeamSessionEventRepository teamSessionEventRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private RoomsUtilityService roomsUtilityService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TeamSessionEventService teamSessionEventService;

    @BeforeEach
    void setUp() {
        teamSessionEventService = new TeamSessionEventService(
            teamSessionEventRepository, sessionRepository, roomsUtilityService, objectMapper);
    }

    // helpers
    private Session session() {
        Game game = new Game();
        game.setName("Skull King");
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        return new Session(room);
    }

    private Team team(Session session) {
        Team team = new Team(session);
        org.springframework.test.util.ReflectionTestUtils.setField(team, "id", UUID.randomUUID());
        return team;
    }

    private TeamSessionEvent existingBid(Session session, SessionEvent sessionEvent, Team team, int bid, int version) {
        TeamSessionEvent row = new TeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BIDS, 7, "{\"bid\":" + bid + "}");
        row.setVersion(version);
        return row;
    }

    private int write(Session session, SessionEvent sessionEvent, Team team, int bid, WriteMode mode, Integer expectedVersion) {
        return teamSessionEventService.writeTeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BIDS,
            new TeamSessionEventPayload.Bids(bid), mode, expectedVersion);
    }

    // --- writeTeamSessionEvent: row shape (any mode) ---

    @Test
    void write_noExistingEvent_insertsWithNextSequence() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());
        when(teamSessionEventRepository.countBySession(session)).thenReturn(3);

        write(session, sessionEvent, team, 2, WriteMode.AUTHORITY, null);

        // A new row keyed on countBySession + 1.
        var captor = org.mockito.ArgumentCaptor.forClass(TeamSessionEvent.class);
        verify(teamSessionEventRepository).save(captor.capture());
        TeamSessionEvent saved = captor.getValue();
        assertEquals(4, saved.getSequence());
        assertEquals(team, saved.getTeam());
        assertTrue(saved.getPayload().contains("\"bid\":2"));
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void write_existingEvent_overwritesPayloadInPlace() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 1, 0);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        write(session, sessionEvent, team, 5, WriteMode.AUTHORITY, null);

        // Same row, corrected payload, sequence untouched. No new sequence lookup.
        assertEquals(7, existing.getSequence());
        assertTrue(existing.getPayload().contains("\"bid\":5"));
        verify(teamSessionEventRepository, never()).countBySession(any());
        verify(teamSessionEventRepository).save(existing);
        verify(roomsUtilityService).updateRoomLastUpdated(session.getRoom());
    }

    @Test
    void write_serializesBonusPayload() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BONUS_POINTS, 0, "{}");
        Team team = team(session);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());
        when(teamSessionEventRepository.countBySession(session)).thenReturn(0);

        TeamSessionEventPayload payload = new TeamSessionEventPayload.BonusPoints(
            2, true, 1, 0, false, 4);
        teamSessionEventService.writeTeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BONUS_POINTS, payload, WriteMode.AUTHORITY, null);

        var captor = org.mockito.ArgumentCaptor.forClass(TeamSessionEvent.class);
        verify(teamSessionEventRepository).save(captor.capture());
        String json = captor.getValue().getPayload();
        assertTrue(json.contains("\"standardFourteens\":2"));
        assertTrue(json.contains("\"blackFourteen\":true"));
        assertTrue(json.contains("\"loot\":4"));
    }

    // --- session lock ---

    @Test
    void write_locksSessionBeforeReadingTheRowOrCountingSequence() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);

        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());

        write(session, sessionEvent, team, 2, WriteMode.CHECKED, 0);

        InOrder inOrder = inOrder(sessionRepository, teamSessionEventRepository);
        inOrder.verify(sessionRepository).lockById(session.getId());
        inOrder.verify(teamSessionEventRepository).findBySessionEventAndTeam(sessionEvent, team);
        inOrder.verify(teamSessionEventRepository).countBySession(session);
        inOrder.verify(teamSessionEventRepository).save(any());
    }

    @Test
    void write_locksSessionForEveryMode() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existingBid(session, sessionEvent, team, 1, 0)));

        for (WriteMode mode : WriteMode.values()) {
            write(session, sessionEvent, team, 2, mode, null);
        }

        verify(sessionRepository, times(WriteMode.values().length)).lockById(session.getId());
    }

    // --- PREFILL ---

    @Test
    void prefill_missingRow_insertsAtVersionZero() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());

        int version = write(session, sessionEvent, team, 0, WriteMode.PREFILL, null);

        assertEquals(0, version);
        var captor = org.mockito.ArgumentCaptor.forClass(TeamSessionEvent.class);
        verify(teamSessionEventRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getVersion());
    }

    @Test
    void prefill_existingRow_isNeverTouched() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 3, 2);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        int version = write(session, sessionEvent, team, 0, WriteMode.PREFILL, null);

        assertEquals(2, version);
        assertEquals(2, existing.getVersion());
        assertTrue(existing.getPayload().contains("\"bid\":3"));
        verify(teamSessionEventRepository, never()).save(any());
        verify(roomsUtilityService, never()).updateRoomLastUpdated(any());
    }

    // --- AUTHORITY ---

    @Test
    void authority_insertsAtVersionOne_thenBumpsOnEachUpdate() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());

        assertEquals(1, write(session, sessionEvent, team, 1, WriteMode.AUTHORITY, null));

        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 1, 1);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        // Even a non-null expected version is ignored: corrections always apply.
        assertEquals(2, write(session, sessionEvent, team, 2, WriteMode.AUTHORITY, 99));
        assertEquals(3, write(session, sessionEvent, team, 3, WriteMode.AUTHORITY, null));
        assertEquals(3, existing.getVersion());
        assertTrue(existing.getPayload().contains("\"bid\":3"));
    }

    // --- CHECKED ---

    @Test
    void checked_matchingVersion_appliesAndBumps() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 1, 2);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        int version = write(session, sessionEvent, team, 4, WriteMode.CHECKED, 2);

        assertEquals(3, version);
        assertEquals(3, existing.getVersion());
        assertTrue(existing.getPayload().contains("\"bid\":4"));
        verify(teamSessionEventRepository).save(existing);
    }

    @Test
    void checked_noRowAndExpectedZero_insertsAtVersionOne() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());

        assertEquals(1, write(session, sessionEvent, team, 4, WriteMode.CHECKED, 0));
    }

    @Test
    void checked_staleVersion_throwsWithCurrentRow_andDoesNotSave() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 3, 2);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        VersionConflictException e = assertThrows(VersionConflictException.class,
            () -> write(session, sessionEvent, team, 1, WriteMode.CHECKED, 1));

        assertEquals(team.getId(), e.getTeamId());
        assertEquals(TeamSessionEventType.BIDS, e.getType());
        assertEquals(2, e.getCurrentVersion());
        assertEquals(new TeamSessionEventPayload.Bids(3), e.getCurrentPayload());
        assertEquals(2, existing.getVersion());
        assertTrue(existing.getPayload().contains("\"bid\":3"));
        verify(teamSessionEventRepository, never()).save(any());
        verify(roomsUtilityService, never()).updateRoomLastUpdated(any());
    }

    @Test
    void checked_staleVersionAgainstMissingRow_throwsAtVersionZeroWithNoPayload() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.empty());

        VersionConflictException e = assertThrows(VersionConflictException.class,
            () -> write(session, sessionEvent, team, 1, WriteMode.CHECKED, 1));

        assertEquals(0, e.getCurrentVersion());
        assertNull(e.getCurrentPayload());
        verify(teamSessionEventRepository, never()).save(any());
    }

    @Test
    void checked_staleBonus_carriesBonusPayload() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BONUS_POINTS, 0, "{}");
        Team team = team(session);
        TeamSessionEventPayload.BonusPoints stored = new TeamSessionEventPayload.BonusPoints(1, true, 0, 2, false, 1);
        TeamSessionEvent existing = new TeamSessionEvent(
            session, sessionEvent, team, TeamSessionEventType.BONUS_POINTS, 1, objectMapper.writeValueAsString(stored));
        existing.setVersion(4);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        VersionConflictException e = assertThrows(VersionConflictException.class,
            () -> teamSessionEventService.writeTeamSessionEvent(
                session, sessionEvent, team, TeamSessionEventType.BONUS_POINTS,
                new TeamSessionEventPayload.BonusPoints(0, false, 0, 0, false, 0), WriteMode.CHECKED, 3));

        assertEquals(4, e.getCurrentVersion());
        assertEquals(stored, e.getCurrentPayload());
    }

    @Test
    void checked_nullVersion_skipsTheCheckAndBumps() {
        Session session = session();
        SessionEvent sessionEvent = new SessionEvent(session, SessionEventType.BIDS, 0, "{}");
        Team team = team(session);
        TeamSessionEvent existing = existingBid(session, sessionEvent, team, 1, 5);
        when(teamSessionEventRepository.findBySessionEventAndTeam(sessionEvent, team))
            .thenReturn(Optional.of(existing));

        assertEquals(6, write(session, sessionEvent, team, 2, WriteMode.CHECKED, null));
        assertTrue(existing.getPayload().contains("\"bid\":2"));
    }
}
