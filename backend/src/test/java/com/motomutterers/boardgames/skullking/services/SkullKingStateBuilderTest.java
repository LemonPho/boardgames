package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.sessions.services.SessionEventService;
import com.motomutterers.boardgames.skullking.dto.BidsStateResponse;
import com.motomutterers.boardgames.skullking.dto.BonusPointsStateResponse;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.dto.TrickResultsStateResponse;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Chunk 5 (builder) — SkullKingStateBuilder builds the per-user phase snapshot
 * the frontend consumes. Covers the response type per phase, the kraken flag,
 * and the per-player visibility filter (non-admins see only their own values).
 */
@ExtendWith(MockitoExtension.class)
public class SkullKingStateBuilderTest {

    @Mock private SessionEventService sessionEventService;
    @Mock private TeamSessionEventRepository teamSessionEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SkullKingStateBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SkullKingStateBuilder(sessionEventService, teamSessionEventRepository, objectMapper);
    }

    // helpers
    private Room room() {
        Game game = new Game();
        game.setName("Skull King");
        return new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
    }

    private Team team(LocalDateTime joinedAt) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", UUID.randomUUID());
        User user = new User("p@test.com", "player", "hash");
        RoomUser roomUser = new RoomUser(user, room(), RoomUserRoles.PLAYER);
        roomUser.setJoinedAt(joinedAt);
        ReflectionTestUtils.setField(team, "roomUser", roomUser);
        return team;
    }

    private Session session(Team... teams) {
        Session session = new Session(room());
        for (Team t : teams) session.addTeam(t);
        return session;
    }

    private RoomUser roomUser(RoomUserRoles role, Team team) {
        RoomUser ru = new RoomUser(new User("u@test.com", "u", "h"), room(), role);
        ru.setTeam(team);
        return ru;
    }

    private SessionEvent bids(int round, int cardCount, String starter) {
        return event(SessionEventType.BIDS, new SessionEventPayload.Bids(round, cardCount, starter));
    }

    private SessionEvent tricks(int round, int cardCount, String starter, boolean kraken) {
        return event(SessionEventType.TRICK_RESULTS, new SessionEventPayload.TrickResults(round, cardCount, starter, kraken));
    }

    private SessionEvent bonus(int round, int cardCount, String starter) {
        return event(SessionEventType.BONUS_POINTS, new SessionEventPayload.BonusPoints(round, cardCount, starter));
    }

    private SessionEvent event(SessionEventType type, SessionEventPayload payload) {
        return new SessionEvent(null, type, 0, objectMapper.writeValueAsString(payload));
    }

    private TeamSessionEvent teamBid(Team team, int bid) {
        return teamEvent(team, TeamSessionEventType.BIDS, new TeamSessionEventPayload.Bids(bid));
    }

    private TeamSessionEvent teamTricks(Team team, int tricksWon) {
        return teamEvent(team, TeamSessionEventType.TRICK_RESULTS, new TeamSessionEventPayload.TrickResults(tricksWon));
    }

    private TeamSessionEvent teamEvent(Team team, TeamSessionEventType type, TeamSessionEventPayload payload) {
        return new TeamSessionEvent(null, null, team, type, 0, objectMapper.writeValueAsString(payload));
    }

    // --- BIDS phase ---

    @Test
    void bidsPhase_returnsBidsStateWithVisibleBids() {
        Team a = team(LocalDateTime.now().minusMinutes(2));
        Team b = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(a, b);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(teamBid(a, 2), teamBid(b, 3)));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertInstanceOf(BidsStateResponse.class, state);
        BidsStateResponse bidsState = (BidsStateResponse) state;
        assertEquals(SessionEventType.BIDS, bidsState.getGameState());
        assertEquals(1, bidsState.getRound());
        assertEquals(5, bidsState.getCardCount());
        // Admin sees every team's bid.
        assertEquals(2, bidsState.getBids().size());
        assertEquals(2, bidsState.getBids().get(a.getId()));
        assertEquals(3, bidsState.getBids().get(b.getId()));
    }

    @Test
    void bidsPhase_nonAdminSeesOnlyOwnBid() {
        Team a = team(LocalDateTime.now().minusMinutes(2));
        Team b = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(a, b);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(teamBid(a, 2), teamBid(b, 3)));

        // Player on team b, not admin.
        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.PLAYER, b), false);

        BidsStateResponse bidsState = (BidsStateResponse) state;
        // Only their own team's bid is visible during bidding.
        assertEquals(1, bidsState.getBids().size());
        assertEquals(3, bidsState.getBids().get(b.getId()));
        assertNull(bidsState.getBids().get(a.getId()));
    }

    // --- TRICK_RESULTS phase ---

    @Test
    void trickResultsPhase_returnsTrickStateWithKrakenFlag() {
        Team a = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(a);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), true);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricksEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(teamBid(a, 2)));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent))
            .thenReturn(List.of(teamTricks(a, 2)));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertInstanceOf(TrickResultsStateResponse.class, state);
        TrickResultsStateResponse trickState = (TrickResultsStateResponse) state;
        assertTrue(trickState.getKrakenPlayed());
        assertEquals(2, trickState.getTrickResults().get(a.getId()));
        assertEquals(2, trickState.getBids().get(a.getId()));
    }

    // --- BONUS_POINTS phase ---

    @Test
    void bonusPhase_returnsBonusStateWithAllMaps() {
        Team a = team(LocalDateTime.now().minusMinutes(1));
        Session session = session(a);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), false);
        SessionEvent bonusEvent = bonus(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonusEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricksEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(teamBid(a, 2)));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent))
            .thenReturn(List.of(teamTricks(a, 2)));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent))
            .thenReturn(List.of(teamEvent(a, TeamSessionEventType.BONUS_POINTS,
                new TeamSessionEventPayload.BonusPoints(1, false, 0, 0, false, 0))));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertInstanceOf(BonusPointsStateResponse.class, state);
        BonusPointsStateResponse bonusState = (BonusPointsStateResponse) state;
        assertEquals(2, bonusState.getBids().get(a.getId()));
        assertEquals(2, bonusState.getTrickResults().get(a.getId()));
        assertEquals(1, bonusState.getBonuses().get(a.getId()).getStandardFourteens());
    }

    // --- team ordering ---

    @Test
    void teamsOrderedByJoinTime() {
        Team later = team(LocalDateTime.now().minusMinutes(1));
        Team earlier = team(LocalDateTime.now().minusMinutes(5));
        // Added out of join order.
        Session session = session(later, earlier);

        SessionEvent bidsEvent = bids(1, 5, earlier.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent)).thenReturn(List.of());

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, earlier), true);

        // Earlier joiner comes first regardless of insertion order.
        assertEquals(earlier.getId(), state.getTeams().get(0).getId());
        assertEquals(later.getId(), state.getTeams().get(1).getId());
    }
}
