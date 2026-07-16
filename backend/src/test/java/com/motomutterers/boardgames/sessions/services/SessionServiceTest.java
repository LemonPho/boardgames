package com.motomutterers.boardgames.sessions.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.CreateSessionRequest;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.exceptions.SessionExistsException;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.skullking.services.SkullKingService;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private SessionUtilitysService sessionUtilitysService;
    @Mock private SkullKingService skullKingService;
    @Mock private RoomsUtilityService roomsUtilityService;
    @Mock private TeamUtilityService teamUtilityService;
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        // eventPublisher is a field-injected @Autowired dependency, not a
        // constructor arg, so @InjectMocks leaves it null. Wire it manually.
        ReflectionTestUtils.setField(sessionService, "eventPublisher", eventPublisher);
    }

    // helpers
    private User user() {
        User user = new User("test@test.com", "testuser", "hash");
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private Room room(RoomStatus status) {
        Game game = new Game();
        game.setName("Skull King");
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(status);
        return room;
    }

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(UUID.randomUUID().toString());
        return auth;
    }

    // --- createSession ---

    @Test
    void createSession_notAdmin_throwsAndSavesNothing() {
        Room room = room(RoomStatus.WAITING);
        User user = user();
        Authentication auth = auth();

        when(userService.getAuthenticatedUser(auth)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        doThrow(new BadActionException("not admin"))
            .when(roomsUtilityService).throwIfUserIsNotRoomAdmin(room, user);

        assertThrows(BadActionException.class,
            () -> sessionService.createSession(new CreateSessionRequest("room"), auth));
        verify(sessionRepository, never()).save(any());
        verify(skullKingService, never()).createInitialRound(any());
    }

    @Test
    void createSession_sessionAlreadyExists_throws() {
        Room room = room(RoomStatus.WAITING);
        User user = user();
        Authentication auth = auth();

        when(userService.getAuthenticatedUser(auth)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        doThrow(new SessionExistsException("exists"))
            .when(sessionUtilitysService).throwIfSessionExistsInRoom(room);

        assertThrows(SessionExistsException.class,
            () -> sessionService.createSession(new CreateSessionRequest("room"), auth));
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createSession_roomNotWaiting_throwsBadAction() {
        Room room = room(RoomStatus.IN_PROGRESS);
        User user = user();
        Authentication auth = auth();

        when(userService.getAuthenticatedUser(auth)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);

        assertThrows(BadActionException.class,
            () -> sessionService.createSession(new CreateSessionRequest("room"), auth));
        verify(sessionRepository, never()).save(any());
        verify(skullKingService, never()).createInitialRound(any());
    }

    @Test
    void createSession_valid_savesSessionMovesRoomInProgressCreatesTeamsAndInitialRound() {
        Room room = room(RoomStatus.WAITING);
        User admin = user();
        room.addPlayer(new RoomUser(admin, room, RoomUserRoles.ADMIN));
        room.addPlayer(new RoomUser(user(), room, RoomUserRoles.PLAYER));
        Authentication auth = auth();

        when(userService.getAuthenticatedUser(auth)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(teamUtilityService.createTeam(any(RoomUser.class), any(Session.class)))
            .thenAnswer(i -> new Team(i.getArgument(1)));

        SessionResponse response = sessionService.createSession(new CreateSessionRequest("room"), auth);

        assertNotNull(response);
        verify(sessionRepository).save(any(Session.class));
        verify(roomsUtilityService).changeRoomStatus(room, RoomStatus.IN_PROGRESS);
        // A team is created for every player in the room.
        verify(teamUtilityService, times(2)).createTeam(any(RoomUser.class), any(Session.class));
        verify(skullKingService).createInitialRound(any(Session.class));
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }
}
