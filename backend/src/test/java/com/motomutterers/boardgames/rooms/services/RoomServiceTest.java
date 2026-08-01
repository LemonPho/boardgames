package com.motomutterers.boardgames.rooms.services;

import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.notifications.services.NotificationService;
import com.motomutterers.boardgames.rooms.dto.CreateAnonymousPlayerRequest;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.MovePlayerRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenExpiredException;
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
import java.util.Optional;
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

    // A verified (ACTIVE) user — invitePlayer requires the invitee to be active.
    private User activeUserWithId(UUID id) {
        User user = userWithId(id);
        ReflectionTestUtils.setField(user, "status", com.motomutterers.boardgames.user.model.UserStatus.ACTIVE);
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
        RoomUser roomUser = new RoomUser(user, room, RoomUserRoles.PLAYER, 0);

        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room)).thenReturn(roomUser);

        roomService.leaveRoom("room", userId);

        verify(roomUserRepository).delete(roomUser);
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- movePlayer ---

    // Builds a room with `count` players seated 0..count-1, with a distinct id per
    // RoomUser so the service can look one up. Returns the players in seat order.
    private List<RoomUser> roomWithSeatedPlayers(Room room, int count) {
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        List<RoomUser> players = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            RoomUser ru = new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER, i);
            ReflectionTestUtils.setField(ru, "id", UUID.randomUUID());
            room.addPlayer(ru);
            players.add(ru);
        }
        return players;
    }

    private MovePlayerRequest moveRequest(UUID adminId, UUID roomUserId, int newLocation) {
        MovePlayerRequest request = new MovePlayerRequest();
        ReflectionTestUtils.setField(request, "adminId", adminId);
        ReflectionTestUtils.setField(request, "roomName", "room");
        ReflectionTestUtils.setField(request, "roomUserId", roomUserId);
        ReflectionTestUtils.setField(request, "newLocation", newLocation);
        return request;
    }

    @Test
    void movePlayer_movingDown_shiftsPlayersBetweenUp() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        List<RoomUser> players = roomWithSeatedPlayers(room, 4);  // seats 0,1,2,3
        RoomUser mover = players.get(0);

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserById(mover.getId())).thenReturn(mover);

        // Move seat 0 → seat 2: players originally at 1,2 shift up to 0,1; mover lands at 2.
        roomService.movePlayer(moveRequest(adminId, mover.getId(), 2));

        assertEquals(2, players.get(0).getPlayingPosition());  // mover
        assertEquals(0, players.get(1).getPlayingPosition());
        assertEquals(1, players.get(2).getPlayingPosition());
        assertEquals(3, players.get(3).getPlayingPosition());  // unaffected
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void movePlayer_movingUp_shiftsPlayersBetweenDown() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        List<RoomUser> players = roomWithSeatedPlayers(room, 4);  // seats 0,1,2,3
        RoomUser mover = players.get(3);

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserById(mover.getId())).thenReturn(mover);

        // Move seat 3 → seat 1: players originally at 1,2 shift down to 2,3; mover lands at 1.
        roomService.movePlayer(moveRequest(adminId, mover.getId(), 1));

        assertEquals(0, players.get(0).getPlayingPosition());  // unaffected
        assertEquals(2, players.get(1).getPlayingPosition());
        assertEquals(3, players.get(2).getPlayingPosition());
        assertEquals(1, players.get(3).getPlayingPosition());  // mover
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void movePlayer_newLocationBeyondEnd_clampsToLastSeat() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        List<RoomUser> players = roomWithSeatedPlayers(room, 3);  // seats 0,1,2
        RoomUser mover = players.get(0);

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserById(mover.getId())).thenReturn(mover);

        // An out-of-range target clamps to the last seat.
        roomService.movePlayer(moveRequest(adminId, mover.getId(), 99));

        assertEquals(2, players.get(0).getPlayingPosition());  // mover, now last
        assertEquals(0, players.get(1).getPlayingPosition());
        assertEquals(1, players.get(2).getPlayingPosition());
    }

    @Test
    void movePlayer_notAdmin_throwsAndDoesNotReorder() {
        UUID adminId = UUID.randomUUID();
        User caller = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        when(userService.getUserById(adminId)).thenReturn(caller);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        doThrow(new BadActionException("not admin"))
            .when(roomsUtilityService).throwIfUserIsNotRoomAdmin(room, caller);

        assertThrows(BadActionException.class,
            () -> roomService.movePlayer(moveRequest(adminId, UUID.randomUUID(), 1)));
        verify(roomUserRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void movePlayer_playerFromAnotherRoom_throws() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        ReflectionTestUtils.setField(room, "id", UUID.randomUUID());
        Room otherRoom = new Room(mockGame(), "other", new RoomConfiguration(TrackingMode.ADMIN, false));
        ReflectionTestUtils.setField(otherRoom, "id", UUID.randomUUID());
        RoomUser foreign = new RoomUser(userWithId(UUID.randomUUID()), otherRoom, RoomUserRoles.PLAYER, 0);
        ReflectionTestUtils.setField(foreign, "id", UUID.randomUUID());

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.getOrThrowRoomUserById(foreign.getId())).thenReturn(foreign);

        assertThrows(BadActionException.class,
            () -> roomService.movePlayer(moveRequest(adminId, foreign.getId(), 0)));
        verify(roomUserRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
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

    // Builds a standard invite request for "bob" to "room" by "admin".
    private RoomInvitationRequest inviteRequest(UUID adminId) {
        RoomInvitationRequest request = new RoomInvitationRequest();
        ReflectionTestUtils.setField(request, "adminId", adminId);
        ReflectionTestUtils.setField(request, "roomName", "room");
        ReflectionTestUtils.setField(request, "username", "bob");
        return request;
    }

    // Common stubbing for invitePlayer up to the status check: admin resolves,
    // invitee resolves and is verified, room resolves and is fresh, not full.
    private void stubInvitePreconditions(UUID adminId, User admin, User invitee, Room room) {
        when(userService.getUserById(adminId)).thenReturn(admin);
        when(userService.getUserByUsername("bob")).thenReturn(invitee);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
    }

    @Test
    void invitePlayer_freshUser_createsPendingInviteRoomUser_andToken() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        User invitee = activeUserWithId(UUID.randomUUID());
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        stubInvitePreconditions(adminId, admin, invitee, room);
        when(roomsUtilityService.getIsUserInActiveRoom(invitee)).thenReturn(false);
        when(roomsUtilityService.getRoomUserStatus(room, invitee)).thenReturn(Optional.empty());
        when(roomsUtilityService.nextPlayingPosition(room)).thenReturn(3);

        roomService.invitePlayer(inviteRequest(adminId));

        // A PENDING_INVITE RoomUser is created at the next seat, and a token points to it.
        verify(roomUserRepository).save(argThat(ru ->
            ru.getStatus() == RoomUserStatus.PENDING_INVITE
                && ru.getRole() == RoomUserRoles.PLAYER
                && ru.getUser().equals(invitee)
                && ru.getPlayingPosition() == 3));
        verify(roomInvitationTokenRepository).save(argThat(t -> t.getRoomUser() != null));
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void invitePlayer_userInAnotherActiveGame_throwsAndDoesNotInvite() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        User invitee = userWithId(UUID.randomUUID());
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        stubInvitePreconditions(adminId, admin, invitee, room);
        when(roomsUtilityService.getIsUserInActiveRoom(invitee)).thenReturn(true);

        BadActionException ex = assertThrows(BadActionException.class,
            () -> roomService.invitePlayer(inviteRequest(adminId)));
        assertEquals("User is in an active session", ex.getMessage());
        verify(roomUserRepository, never()).save(any());
        verify(roomInvitationTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void invitePlayer_alreadyActiveInRoom_throwsWithMessage() {
        assertInviteRejectedForStatus(RoomUserStatus.ACTIVE, "User is already in the room");
    }

    @Test
    void invitePlayer_alreadyInvited_throwsWithMessage() {
        assertInviteRejectedForStatus(RoomUserStatus.PENDING_INVITE, "User is already invited");
    }

    @Test
    void invitePlayer_previouslyDeclined_throwsWithMessage() {
        assertInviteRejectedForStatus(RoomUserStatus.DECLINED, "User has declined an invite to this room");
    }

    // Drives invitePlayer with an existing RoomUser in the given status and asserts
    // it's rejected with the expected message and nothing is persisted.
    private void assertInviteRejectedForStatus(RoomUserStatus status, String expectedMessage) {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        User invitee = activeUserWithId(UUID.randomUUID());
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));

        stubInvitePreconditions(adminId, admin, invitee, room);
        when(roomsUtilityService.getIsUserInActiveRoom(invitee)).thenReturn(false);
        when(roomsUtilityService.getRoomUserStatus(room, invitee)).thenReturn(Optional.of(status));

        BadActionException ex = assertThrows(BadActionException.class,
            () -> roomService.invitePlayer(inviteRequest(adminId)));
        assertEquals(expectedMessage, ex.getMessage());
        verify(roomUserRepository, never()).save(any());
        verify(roomInvitationTokenRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- revokeInvite ---

    @Test
    void revokeInvite_deletesRoomUser_andDismissesNotification() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        User invitee = userWithId(UUID.randomUUID());
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser invited = new RoomUser(invitee, room, RoomUserStatus.PENDING_INVITE, 1);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().plusHours(1));

        when(userService.getUserById(adminId)).thenReturn(admin);
        when(userService.getUserByUsername("bob")).thenReturn(invitee);
        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
        when(roomsUtilityService.getOrThrowRoomUserByUserAndRoom(invitee, room)).thenReturn(invited);
        when(roomsUtilityService.getOrThrowRoomInvitationTokenByRoomUser(invited)).thenReturn(token);

        roomService.revokeInvite(inviteRequest(adminId));

        // Removing the RoomUser cascades to its token; the notification is dismissed.
        verify(roomUserRepository).delete(invited);
        verify(notificationService).deleteInvitation("tok");
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- declineInvite ---

    @Test
    void declineInvite_marksDeclined_deletesToken_keepsRoomUser() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser invited = new RoomUser(user, room, RoomUserStatus.PENDING_INVITE, 1);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().plusHours(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);

        roomService.declineInvite("tok", auth);

        // Declining is recorded (anti-spam) and the token removed; the RoomUser stays.
        assertEquals(RoomUserStatus.DECLINED, invited.getStatus());
        verify(roomUserRepository).save(invited);
        verify(roomUserRepository, never()).delete(any());
        verify(roomInvitationTokenRepository).delete(token);
        verify(notificationService).deleteInvitation("tok");
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void declineInvite_wrongUser_throwsAndDoesNotChangeStatus() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User owner = userWithId(ownerId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser invited = new RoomUser(owner, room, RoomUserStatus.PENDING_INVITE, 1);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().plusHours(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(otherId.toString());
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);

        assertThrows(BadActionException.class, () -> roomService.declineInvite("tok", auth));
        assertEquals(RoomUserStatus.PENDING_INVITE, invited.getStatus());
        verify(roomInvitationTokenRepository, never()).delete(any());
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
        room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER, 0));
        room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER, 1));

        // The invited player's RoomUser exists up front (PENDING_INVITE); the token
        // points at it.
        RoomUser invited = new RoomUser(user, room, RoomUserStatus.PENDING_INVITE, 2);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().plusHours(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);
        doThrow(new BadActionException("Room is full"))
            .when(roomsUtilityService).throwIfPlayerLimitReached(room);

        assertThrows(BadActionException.class, () -> roomService.acceptInvite("tok", auth));
        // Rejected at the cap: the invite is not converted (no token deletion) and
        // no room update is broadcast. The player stays PENDING_INVITE.
        verify(roomInvitationTokenRepository, never()).delete(any());
        assertEquals(RoomUserStatus.PENDING_INVITE, invited.getStatus());
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void acceptInvite_success_activatesPlayer_deletesToken_dismissesNotification() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser invited = new RoomUser(user, room, RoomUserStatus.PENDING_INVITE, 1);
        room.addPlayer(invited);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().plusHours(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);

        roomService.acceptInvite("tok", auth);

        // The reserved seat converts to a joined player, the token is deleted (no
        // lingering backlog), and the notification is dismissed.
        assertEquals(RoomUserStatus.ACTIVE, invited.getStatus());
        verify(roomUserRepository).save(invited);
        verify(roomInvitationTokenRepository).delete(token);
        verify(notificationService).markInvitationRead("tok");
        verify(eventPublisher).publishEvent(any(RoomUpdatedEvent.class));
    }

    @Test
    void acceptInvite_expiredToken_removesInvitedPlayer_andThrows() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        RoomUser invited = new RoomUser(user, room, RoomUserStatus.PENDING_INVITE, 1);
        RoomInvitationToken token = new RoomInvitationToken(invited, room, "tok", LocalDateTime.now().minusDays(1));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId.toString());
        when(userService.getUserById(userId)).thenReturn(user);
        when(roomsUtilityService.getRoomInvitationTokenByToken("tok")).thenReturn(token);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);

        assertThrows(RoomInvitationTokenExpiredException.class, () -> roomService.acceptInvite("tok", auth));
        // The stale invite is dropped entirely (cascades to the token).
        verify(roomUserRepository).delete(invited);
        verify(eventPublisher, never()).publishEvent(any(RoomUpdatedEvent.class));
    }

    // --- getRoom ---

    @Test
    void getRoom_returnsResponse() {
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(RoomStatus.WAITING);

        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);

        RoomResponse response = roomService.getRoom("room");

        assertEquals("room", response.getName());
    }

    @Test
    void getRoom_excludesDeclinedPlayers() {
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(RoomStatus.WAITING);
        User activeUser = userWithId(UUID.randomUUID());
        User pendingUser = userWithId(UUID.randomUUID());
        User declinedUser = userWithId(UUID.randomUUID());
        ReflectionTestUtils.setField(activeUser, "username", "activeUser");
        ReflectionTestUtils.setField(pendingUser, "username", "pendingUser");
        ReflectionTestUtils.setField(declinedUser, "username", "declinedUser");
        room.addPlayer(new RoomUser(activeUser, room, RoomUserRoles.ADMIN, 0));
        room.addPlayer(new RoomUser(pendingUser, room, RoomUserStatus.PENDING_INVITE, 1));
        room.addPlayer(new RoomUser(declinedUser, room, RoomUserStatus.DECLINED, 2));

        when(roomsUtilityService.getRoomByName("room")).thenReturn(room);
        when(roomsUtilityService.isRoomExpired(room)).thenReturn(false);

        RoomResponse response = roomService.getRoom("room");

        // Active + pending invites are surfaced; the declined player is hidden.
        assertEquals(2, response.getPlayers().size());
        assertTrue(response.getPlayers().stream()
            .noneMatch(p -> p.getStatus() == RoomUserStatus.DECLINED));
        assertTrue(response.getPlayers().stream()
            .anyMatch(p -> p.getStatus() == RoomUserStatus.PENDING_INVITE));
    }
}
