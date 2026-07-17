package com.motomutterers.boardgames.skullking.services;

import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.sessions.services.SessionEventService;
import com.motomutterers.boardgames.sessions.services.SessionUtilitysService;
import com.motomutterers.boardgames.sessions.services.TeamSessionEventService;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Shared fixture for the SkullKingService test suite.
 *
 * The engine reads an event graph through mocked repositories: session events
 * (BIDS → IN_PROGRESS → TRICK_RESULTS → BONUS_POINTS) via SessionEventService,
 * and the per-team rows under each session event via TeamSessionEventRepository.
 * These helpers build that graph with real JSON payloads (a live ObjectMapper,
 * not a mock) so serialization is genuinely exercised, and stub the repos to
 * return it. Subclasses extend this and add their concern-specific tests.
 */
@ExtendWith(MockitoExtension.class)
abstract class SkullKingTestSupport {

    @Mock protected UserService userService;
    @Mock protected RoomsUtilityService roomsUtilityService;
    @Mock protected SessionUtilitysService sessionUtilitysService;
    @Mock protected TeamUtilityService teamUtilityService;
    @Mock protected SessionEventService sessionEventService;
    @Mock protected TeamSessionEventService teamSessionEventService;
    @Mock protected TeamSessionEventRepository teamSessionEventRepository;
    @Mock protected SkullKingStateBuilder stateBuilder;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    protected ApplicationEventPublisher eventPublisher;

    protected SkullKingService service;

    @BeforeEach
    void wireService() {
        // Construct with a real ObjectMapper so JSON (de)serialization is exercised
        // rather than mocked. eventPublisher is a field-injected @Autowired
        // dependency, so set it by hand after construction.
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new SkullKingService(
            userService, roomsUtilityService, sessionUtilitysService, teamUtilityService,
            sessionEventService, teamSessionEventService, teamSessionEventRepository,
            stateBuilder, objectMapper);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
    }

    // --- domain builders ---

    protected Room room(boolean advanced) {
        Game game = new Game();
        game.setName("Skull King");
        game.setMaxPlayers(8);
        return new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, advanced));
    }

    protected Session session(boolean advanced, Team... teams) {
        Session session = new Session(room(advanced));
        for (Team team : teams) {
            session.addTeam(team);
        }
        return session;
    }

    /** A team with a stable id; join order (for rotation tests) is set via joinedAt. */
    protected Team team() {
        return team(null);
    }

    protected Team team(LocalDateTime joinedAt) {
        Team team = new Team();
        ReflectionTestUtils.setField(team, "id", UUID.randomUUID());
        if (joinedAt != null) {
            User user = new User("p@test.com", "player", "hash");
            RoomUser roomUser = new RoomUser(user, room(false), RoomUserRoles.PLAYER);
            roomUser.setJoinedAt(joinedAt);
            ReflectionTestUtils.setField(team, "roomUser", roomUser);
        }
        return team;
    }

    protected String id(Team team) {
        return team.getId().toString();
    }

    // --- session event builders (real JSON payloads) ---

    protected SessionEvent bidsEvent(int round, int cardCount, String startingTeamId) {
        return event(SessionEventType.BIDS, new SessionEventPayload.Bids(round, cardCount, startingTeamId));
    }

    protected SessionEvent inProgressEvent(int round, int cardCount, String startingTeamId) {
        return event(SessionEventType.IN_PROGRESS, new SessionEventPayload.InProgress(round, cardCount, startingTeamId));
    }

    protected SessionEvent tricksEvent(int round, int cardCount, String startingTeamId, boolean krakenPlayed) {
        return event(SessionEventType.TRICK_RESULTS, new SessionEventPayload.TrickResults(round, cardCount, startingTeamId, krakenPlayed));
    }

    protected SessionEvent bonusEvent(int round, int cardCount, String startingTeamId) {
        return event(SessionEventType.BONUS_POINTS, new SessionEventPayload.BonusPoints(round, cardCount, startingTeamId));
    }

    protected SessionEvent event(SessionEventType type, SessionEventPayload payload) {
        return new SessionEvent(null, type, 0, objectMapper.writeValueAsString(payload));
    }

    // --- team session event builders ---

    protected TeamSessionEvent teamBid(Team team, int bid) {
        return teamEvent(team, TeamSessionEventType.BIDS, new TeamSessionEventPayload.Bids(bid));
    }

    protected TeamSessionEvent teamTricks(Team team, int tricksWon) {
        return teamEvent(team, TeamSessionEventType.TRICK_RESULTS, new TeamSessionEventPayload.TrickResults(tricksWon));
    }

    protected TeamSessionEvent teamBonus(Team team, TeamSessionEventPayload.BonusPoints bonus) {
        return teamEvent(team, TeamSessionEventType.BONUS_POINTS, bonus);
    }

    protected TeamSessionEvent teamEvent(Team team, TeamSessionEventType type, TeamSessionEventPayload payload) {
        return new TeamSessionEvent(null, null, team, type, 0, objectMapper.writeValueAsString(payload));
    }

    protected TeamSessionEventPayload.BonusPoints bonus(
        int standardFourteens, boolean blackFourteen, int mermaidsByPirate,
        int piratesBySkullKing, boolean skullKingByMermaid, int loot
    ) {
        return new TeamSessionEventPayload.BonusPoints(
            standardFourteens, blackFourteen, mermaidsByPirate, piratesBySkullKing, skullKingByMermaid, loot);
    }

    // --- graph stubs (lenient: shared fixture may stub more than a given test reads) ---

    protected void stubAllEvents(Session session, SessionEvent... events) {
        lenient().when(sessionEventService.findAllEvents(session)).thenReturn(List.of(events));
    }

    protected void stubTeamEvents(SessionEvent sessionEvent, TeamSessionEvent... teamEvents) {
        lenient().when(teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(sessionEvent))
            .thenReturn(List.of(teamEvents));
    }

    protected Authentication auth() {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(UUID.randomUUID().toString());
        return authentication;
    }
}
