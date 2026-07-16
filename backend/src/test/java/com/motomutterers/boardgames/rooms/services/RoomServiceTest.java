package com.motomutterers.boardgames.rooms.services;

import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.notifications.services.NotificationService;
import com.motomutterers.boardgames.rooms.dto.CreateAnonymousPlayerRequest;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock private RoomsUtilityService roomsUtilityService;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomUserRepository roomUserRepository;
    @Mock private GameService gameService;
    @Mock private UserService userService;
    @Mock private RoomInvitationTokenRepository roomInvitationTokenRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RoomService roomService;

    @BeforeEach
    void setUp() {
        // eventPublisher is a field-injected @Autowired dependency, not a
        // constructor arg. @InjectMocks uses constructor injection here and
        // does NOT also field-inject, so wire it up manually.
        ReflectionTestUtils.setField(roomService, "eventPublisher", eventPublisher);
    }

    // helpers
    private User userWithId(UUID id) {
        User user = new User("test@test.com", "testuser", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Game mockGame() {
        Game game = new Game();
        game.setName("Skull King");
        return game;
    }

    private CreateRoomRequest request(TrackingMode mode, boolean advanced) {
        return new CreateRoomRequest("Skull King", new RoomConfiguration(mode, advanced));
    }

    // --- createRoom ---

    @Test
    void createRoom_baseNameFree_usesBaseName() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(userWithId(userId));
        when(gameService.getGameByName("Skull King")).thenReturn(mockGame());
        when(roomRepository.existsByName("testuser's Skull King Room")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.createRoom(request(TrackingMode.ADMIN, false), userId);

        assertEquals("testuser's Skull King Room", response.getName());
    }

    @Test
    void createRoom_baseNameTaken_appendsSuffix() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(userWithId(userId));
        when(gameService.getGameByName("Skull King")).thenReturn(mockGame());
        when(roomRepository.existsByName("testuser's Skull King Room")).thenReturn(true);
        when(roomRepository.existsByName("testuser's Skull King Room 2")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.createRoom(request(TrackingMode.ADMIN, false), userId);

        assertEquals("testuser's Skull King Room 2", response.getName());
    }

    @Test
    void createRoom_persistsConfiguration() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(userWithId(userId));
        when(gameService.getGameByName("Skull King")).thenReturn(mockGame());
        when(roomRepository.existsByName(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        roomService.createRoom(request(TrackingMode.SELF, true), userId);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        RoomConfiguration config = captor.getValue().getConfiguration();
        assertEquals(TrackingMode.SELF, config.getTrackingMode());
        assertTrue(config.getAdvancedCards());
    }

    @Test
    void createRoom_addsCreatorAsAdmin() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(mockGame());
        when(roomRepository.existsByName(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        roomService.createRoom(request(TrackingMode.ADMIN, false), userId);

        verify(roomUserRepository).save(argThat(ru ->
            ru.getRole() == RoomUserRoles.ADMIN && ru.getUser().equals(user)));
    }

    @Test
    void createRoom_userAlreadyInActiveRoom_throws() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(mockGame());
        doThrow(new UserInActiveRoomException("in active room"))
            .when(roomsUtilityService).throwIsUserInActiveRoom(user);

        assertThrows(UserInActiveRoomException.class,
            () -> roomService.createRoom(request(TrackingMode.ADMIN, false), userId));
        verify(roomRepository, never()).save(any());
    }

    // --- cancelRoom ---

    @Test
    void cancelRoom_admin_cancelsAndBroadcasts() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);

        roomService.cancelRoom("room", userId);

        verify(roomsUtilityService).throwIfUserIsNotRoomAdmin(room, user);
        verify(roomsUtilityService).cancelRoom(room);
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- leaveRoom ---

    @Test
    void leaveRoom_deletesRoomUserAndBroadcasts() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser roomUser = new RoomUser(user, room, RoomUserRoles.PLAYER);

        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room)).thenReturn(roomUser);

        roomService.leaveRoom("room", userId);

        verify(roomUserRepository).delete(roomUser);
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- createAnonymousPlayer ---

    @Test
    void createAnonymousPlayer_admin_savesAnonymous() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);

        CreateAnonymousPlayerRequest request = new CreateAnonymousPlayerRequest();
        ReflectionTestUtils.setField(request, "adminId", adminId);
        ReflectionTestUtils.setField(request, "roomName", "room");
        ReflectionTestUtils.setField(request, "displayName", "Ghost");

        roomService.createAnonymousPlayer(request);

        verify(roomsUtilityService).throwIfUserIsNotRoomAdmin(room, admin);
        verify(roomUserRepository).save(argThat(ru ->
            "Ghost".equals(ru.getDisplayName()) && ru.getRole() == RoomUserRoles.ANONYMOUS));
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void createAnonymousPlayer_roomFull_throwsAndDoesNotSave() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        doThrow(new BadActionException("Room is full"))
            .when(roomsUtilityService).throwIfRoomIsFull(room);

        CreateAnonymousPlayerRequest request = new CreateAnonymousPlayerRequest();
        ReflectionTestUtils.setField(request, "adminId", adminId);
        ReflectionTestUtils.setField(request, "roomName", "room");
        ReflectionTestUtils.setField(request, "displayName", "Ghost");

        assertThrows(BadActionException.class, () -> roomService.createAnonymousPlayer(request));
        verify(roomUserRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- invitePlayer ---

    @Test
    void invitePlayer_roomFull_throwsAndDoesNotSaveInvite() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        User invitee = userWithId(UUID.randomUUID());
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(userService.getUserByUsername("bob")).thenReturn(invitee);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
        doThrow(new BadActionException("Room is full"))
            .when(roomsUtilityService).throwIfRoomIsFull(room);

        RoomInvitationRequest request = new RoomInvitationRequest();
        ReflectionTestUtils.setField(request, "adminId", adminId);
        ReflectionTestUtils.setField(request, "roomName", "room");
        ReflectionTestUtils.setField(request, "username", "bob");

        assertThrows(BadActionException.class, () -> roomService.invitePlayer(request));
        verify(roomInvitationTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- acceptInvite ---

    @Test
    void acceptInvite_roomAtPlayerLimit_throwsAndDoesNotJoin() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Game game = mockGame();
        game.setMaxPlayers(2);
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        // Fill the room to its player cap.
        room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER));
        room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER));

        RoomInvitationToken token = new RoomInvitationToken(user, room, "tok", LocalDateTime.now().plusHours(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
        doThrow(new BadActionException("Room is full"))
            .when(roomsUtilityService).throwIfPlayerLimitReached(room);

        assertThrows(BadActionException.class, () -> roomService.acceptInvite("tok", auth));
        verify(roomUserRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- getRoom ---

    @Test
    void getRoom_returnsResponseWithInvitations() {
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(RoomStatus.WAITING);

        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
        when(roomInvitationTokenRepository.findAllByRoomAndStatus(eq(room), any())).thenReturn(List.of());

        RoomResponse response = roomService.getRoom("room");

        assertEquals("room", response.getName());
    }
}
