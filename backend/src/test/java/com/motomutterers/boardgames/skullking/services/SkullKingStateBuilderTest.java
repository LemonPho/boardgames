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
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

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
        // orderedTeams() is a pure sort over the session's teams — it touches
        // neither repository — so a repo-less instance is fine here.
        TeamUtilityService teamUtilityService = new TeamUtilityService(null, null);
        builder = new SkullKingStateBuilder(sessionEventService, teamSessionEventRepository, objectMapper, teamUtilityService);
    }

    // helpers
    private Room room() {
        Game game = new Game();
        game.setName("Skull King");
        return new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
    }

    // A team seated at the given playing position (teams are ordered by it).
    private Team team(int playingPosition) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", UUID.randomUUID());
        User user = new User("p@test.com", "player", "hash");
        RoomUser roomUser = new RoomUser(user, room(), RoomUserRoles.PLAYER, playingPosition);
        ReflectionTestUtils.setField(team, "roomUser", roomUser);
        return team;
    }

    private Session session(Team... teams) {
        Session session = new Session(room());
        for (Team t : teams) session.addTeam(t);
        return session;
    }

    private RoomUser roomUser(RoomUserRoles role, Team team) {
        RoomUser ru = new RoomUser(new User("u@test.com", "u", "h"), room(), role, 0);
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
        SessionEvent event = new SessionEvent(null, type, 0, objectMapper.writeValueAsString(payload));
        // Persisted events always have an id; the builder exposes the BIDS event's as roundKey.
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        return event;
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
        Team a = team(0);
        Team b = team(1);
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
        Team a = team(0);
        Team b = team(1);
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

    // Each phase response type declares its own bids map; there's no shared getter.
    private java.util.Map<UUID, Integer> bidsOf(SkullKingStateResponse state) {
        if (state instanceof BidsStateResponse s) return s.getBids();
        if (state instanceof TrickResultsStateResponse s) return s.getBids();
        if (state instanceof BonusPointsStateResponse s) return s.getBids();
        throw new AssertionError("unexpected state type " + state.getClass());
    }

    @Test
    void bidVisibility_playerSeesAllBidsOnceBiddingEnds_versionsStayOwnTeam() {
        Team a = team(0);
        Team b = team(1);
        Session session = session(a, b);
        String starter = a.getId().toString();

        SessionEvent bidsEvent = bids(1, 5, starter);
        SessionEvent inProgressEvent = event(SessionEventType.IN_PROGRESS, new SessionEventPayload.InProgress(1, 5, starter));
        SessionEvent tricksEvent = tricks(1, 5, starter, false);
        SessionEvent bonusEvent = bonus(1, 5, starter);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        lenient().when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricksEvent));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(versioned(teamBid(a, 2), 3), versioned(teamBid(b, 1), 5)));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent)).thenReturn(List.of());
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent)).thenReturn(List.of());

        for (SessionEvent current : List.of(bidsEvent, inProgressEvent, tricksEvent, bonusEvent)) {
            when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(current);
            String phase = current.getType().toString();

            SkullKingStateResponse player = builder.buildState(session, roomUser(RoomUserRoles.PLAYER, b), false);
            // Bids are secret while bidding and public from IN_PROGRESS onward.
            java.util.Map<UUID, Integer> expectedPlayerBids = current == bidsEvent
                ? java.util.Map.of(b.getId(), 1)
                : java.util.Map.of(a.getId(), 2, b.getId(), 1);
            assertEquals(expectedPlayerBids, bidsOf(player), phase);
            // Players only ever save their own bid, so they only get its version.
            assertEquals(java.util.Map.of(b.getId(), 5), player.getBidVersions(), phase);

            SkullKingStateResponse admin = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);
            assertEquals(java.util.Map.of(a.getId(), 2, b.getId(), 1), bidsOf(admin), phase);
            assertEquals(java.util.Map.of(a.getId(), 3, b.getId(), 5), admin.getBidVersions(), phase);
        }
    }

    // --- roundKey ---

    @Test
    void roundKey_isTheLatestBidsEventId_inEveryPhase() {
        Team a = team(0);
        Session session = session(a);

        // A later phase of the round still reports the round's BIDS event id, so the key only
        // changes on a new round or a reset (which recreates the BIDS event).
        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), false);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricksEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent)).thenReturn(List.of());
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent)).thenReturn(List.of());

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertEquals(bidsEvent.getId().toString(), state.getRoundKey());
    }

    @Test
    void phaseEventId_isTheCurrentEventId_inEveryPhase() {
        Team a = team(0);
        Session session = session(a);
        String starter = a.getId().toString();

        SessionEvent bidsEvent = bids(1, 5, starter);
        SessionEvent inProgressEvent = event(SessionEventType.IN_PROGRESS, new SessionEventPayload.InProgress(1, 5, starter));
        SessionEvent tricksEvent = tricks(1, 5, starter, false);
        SessionEvent bonusEvent = bonus(1, 5, starter);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        lenient().when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricksEvent));
        for (SessionEvent e : List.of(bidsEvent, tricksEvent, bonusEvent)) {
            lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(e)).thenReturn(List.of());
        }

        // Clients echo this id on submits, so it must name the live phase, not the round.
        for (SessionEvent current : List.of(bidsEvent, inProgressEvent, tricksEvent, bonusEvent)) {
            when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(current);
            SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);
            assertEquals(current.getId().toString(), state.getPhaseEventId(), current.getType().toString());
        }
    }

    // --- TRICK_RESULTS phase ---

    @Test
    void trickResultsPhase_returnsTrickStateWithKrakenFlag() {
        Team a = team(0);
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
        Team a = team(0);
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
        assertFalse(bonusState.getKrakenPlayed());
    }

    @Test
    void bonusPhase_carriesKrakenFlagFromTrickResults() {
        Team a = team(0);
        Session session = session(a);

        // The admin can review the tricks during the bonus phase, so the state must carry
        // the round's Kraken flag (it sets the expected trick total).
        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), true);
        SessionEvent bonusEvent = bonus(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonusEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricksEvent));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent)).thenReturn(List.of());
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent)).thenReturn(List.of());
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent)).thenReturn(List.of());

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertTrue(((BonusPointsStateResponse) state).getKrakenPlayed());
    }

    // --- team ordering ---

    @Test
    void teamsOrderedByPlayingPosition() {
        Team second = team(1);
        Team first = team(0);
        // Added out of seat order.
        Session session = session(second, first);

        SessionEvent bidsEvent = bids(1, 5, first.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent)).thenReturn(List.of());

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, first), true);

        // Lower seat position comes first regardless of insertion order.
        assertEquals(first.getId(), state.getTeams().get(0).getId());
        assertEquals(second.getId(), state.getTeams().get(1).getId());
    }

    // --- version maps ---

    private TeamSessionEvent versioned(TeamSessionEvent row, int version) {
        row.setVersion(version);
        return row;
    }

    @Test
    void versionMaps_bonusPhase_adminSeesEveryTeamFromTheSamePhaseEvents() {
        Team a = team(0);
        Team b = team(1);
        Session session = session(a, b);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), false);
        SessionEvent bonusEvent = bonus(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bonusEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS)).thenReturn(Optional.of(tricksEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(versioned(teamBid(a, 2), 3), versioned(teamBid(b, 1), 0)));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent))
            .thenReturn(List.of(versioned(teamTricks(a, 2), 1), versioned(teamTricks(b, 3), 2)));
        // Only a has a bonus row: b has no entry, which clients read as version 0.
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent))
            .thenReturn(List.of(versioned(teamEvent(a, TeamSessionEventType.BONUS_POINTS,
                new TeamSessionEventPayload.BonusPoints(1, false, 0, 0, false, 0)), 4)));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.ADMIN, a), true);

        assertEquals(java.util.Map.of(a.getId(), 3, b.getId(), 0), state.getBidVersions());
        assertEquals(java.util.Map.of(a.getId(), 1, b.getId(), 2), state.getTrickVersions());
        assertEquals(java.util.Map.of(a.getId(), 4), state.getBonusVersions());
    }

    @Test
    void versionMaps_trickPhase_playerSeesOnlyOwnTeam() {
        Team a = team(0);
        Team b = team(1);
        Session session = session(a, b);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        SessionEvent tricksEvent = tricks(1, 5, a.getId().toString(), false);
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(tricksEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(versioned(teamBid(a, 2), 3), versioned(teamBid(b, 1), 5)));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(tricksEvent))
            .thenReturn(List.of(versioned(teamTricks(a, 2), 1), versioned(teamTricks(b, 3), 2)));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.PLAYER, b), false);

        assertEquals(java.util.Map.of(b.getId(), 5), state.getBidVersions());
        assertEquals(java.util.Map.of(b.getId(), 2), state.getTrickVersions());
        // No bonus phase yet: an empty map, not null.
        assertEquals(java.util.Map.of(), state.getBonusVersions());
    }

    @Test
    void versionMaps_bidsPhase_onlyBidVersions() {
        Team a = team(0);
        Session session = session(a);

        SessionEvent bidsEvent = bids(1, 5, a.getId().toString());
        when(sessionEventService.getOrThrowCurrentEvent(session)).thenReturn(bidsEvent);
        when(sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS)).thenReturn(Optional.of(bidsEvent));
        when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent))
            .thenReturn(List.of(versioned(teamBid(a, 2), 2)));

        SkullKingStateResponse state = builder.buildState(session, roomUser(RoomUserRoles.PLAYER, a), false);

        assertEquals(java.util.Map.of(a.getId(), 2), state.getBidVersions());
        assertEquals(java.util.Map.of(), state.getTrickVersions());
        assertEquals(java.util.Map.of(), state.getBonusVersions());
    }
}
