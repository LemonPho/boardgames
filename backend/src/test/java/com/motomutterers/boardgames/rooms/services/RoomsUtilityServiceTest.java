package com.motomutterers.boardgames.rooms.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.notifications.repositories.NotificationRepository;
import com.motomutterers.boardgames.rooms.exceptions.RoomExpiredException;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomUserNotFoundException;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
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
import com.motomutterers.boardgames.sessions.services.SessionUtilitysService;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomsUtilityServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomUserRepository roomUserRepository;
    @Mock private RoomInvitationTokenRepository roomInvitationTokenRepository;
    @Mock private SessionUtilitysService sessionUtilitysService;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private RoomsUtilityService roomsUtilityService;

    private static final int WAITING_EXPIRATION = 900;      // 15 min
    private static final int IN_PROGRESS_EXPIRATION = 3600;  // 1 hr

    @BeforeEach
    void setUp() {
        // @Value fields aren't injected in a plain Mockito test.
        ReflectionTestUtils.setField(roomsUtilityService, "roomWaitingExpiration", WAITING_EXPIRATION);
        ReflectionTestUtils.setField(roomsUtilityService, "roomInProgressExpiration", IN_PROGRESS_EXPIRATION);
    }

    // helpers
    private User userWithId(UUID id) {
        User user = new User("test@test.com", "testuser", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Game mockGame() {
        return gameWithMax(8);
    }

    private Game gameWithMax(int maxPlayers) {
        Game game = new Game();
        game.setName("Skull King");
        game.setMaxPlayers(maxPlayers);
        return game;
    }

    private Room roomWithGame(Game game) {
        Room room = new Room(game, "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(RoomStatus.WAITING);
        room.setLastUpdated(LocalDateTime.now());
        return room;
    }

    private void addPlayers(Room room, int count) {
        for (int i = 0; i < count; i++) {
            room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.PLAYER));
        }
    }

    private Room room(RoomStatus status, LocalDateTime lastUpdated) {
        Room room = new Room(mockGame(), "room", new RoomConfiguration(TrackingMode.ADMIN, false));
        room.setStatus(status);
        room.setLastUpdated(lastUpdated);
        return room;
    }

    // --- getRoomById ---

    @Test
    void getRoomById_exists_returnsRoom() {
        UUID id = UUID.randomUUID();
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomRepository.findById(id)).thenReturn(Optional.of(room));

        assertEquals(room, roomsUtilityService.getRoomById(id));
    }

    @Test
    void getRoomById_missing_throws() {
        UUID id = UUID.randomUUID();
        when(roomRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomsUtilityService.getRoomById(id));
    }

    // --- getRoomByName ---

    @Test
    void getRoomByName_existsAndFresh_returnsRoom() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomRepository.findByName("room")).thenReturn(Optional.of(room));

        assertEquals(room, roomsUtilityService.getRoomByName("room"));
    }

    @Test
    void getRoomByName_missing_throws() {
        when(roomRepository.findByName("nope")).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomsUtilityService.getRoomByName("nope"));
    }

    @Test
    void getRoomByName_expired_cancelsAndThrows() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now().minusSeconds(WAITING_EXPIRATION + 60));
        when(roomRepository.findByName("room")).thenReturn(Optional.of(room));
        when(roomInvitationTokenRepository.findRoomInvitations(room)).thenReturn(List.of());

        assertThrows(RoomExpiredException.class, () -> roomsUtilityService.getRoomByName("room"));
        assertEquals(RoomStatus.CANCELLED, room.getStatus());
    }

    // --- isRoomExpired ---

    @Test
    void isRoomExpired_waitingRecent_false() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now().minusSeconds(10));
        assertFalse(roomsUtilityService.isRoomExpired(room));
    }

    @Test
    void isRoomExpired_waitingStale_true() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now().minusSeconds(WAITING_EXPIRATION + 1));
        assertTrue(roomsUtilityService.isRoomExpired(room));
    }

    @Test
    void isRoomExpired_inProgressUsesLongerWindow() {
        // Older than the waiting window but within the in-progress window → not expired.
        Room room = room(RoomStatus.IN_PROGRESS, LocalDateTime.now().minusSeconds(WAITING_EXPIRATION + 60));
        assertFalse(roomsUtilityService.isRoomExpired(room));
    }

    @Test
    void isRoomExpired_inProgressStale_true() {
        Room room = room(RoomStatus.IN_PROGRESS, LocalDateTime.now().minusSeconds(IN_PROGRESS_EXPIRATION + 1));
        assertTrue(roomsUtilityService.isRoomExpired(room));
    }

    @Test
    void isRoomExpired_completedOrCancelled_neverExpires() {
        Room completed = room(RoomStatus.COMPLETED, LocalDateTime.now().minusDays(30));
        Room cancelled = room(RoomStatus.CANCELLED, LocalDateTime.now().minusDays(30));
        assertFalse(roomsUtilityService.isRoomExpired(completed));
        assertFalse(roomsUtilityService.isRoomExpired(cancelled));
    }

    // --- throwIfUserIsNotRoomAdmin ---

    @Test
    void throwIfUserIsNotRoomAdmin_admin_passes() {
        UUID adminId = UUID.randomUUID();
        User admin = userWithId(adminId);
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        room.addPlayer(new RoomUser(admin, room, RoomUserRoles.ADMIN));

        assertDoesNotThrow(() -> roomsUtilityService.throwIfUserIsNotRoomAdmin(room, admin));
    }

    @Test
    void throwIfUserIsNotRoomAdmin_nonAdmin_throws() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        room.addPlayer(new RoomUser(userWithId(UUID.randomUUID()), room, RoomUserRoles.ADMIN));

        User other = userWithId(UUID.randomUUID());
        assertThrows(BadActionException.class, () -> roomsUtilityService.throwIfUserIsNotRoomAdmin(room, other));
    }

    // --- getOrThrowRoomUserByUserAndRoom ---

    @Test
    void getOrThrowRoomUserByUserAndRoom_missing_throws() {
        User user = userWithId(UUID.randomUUID());
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomUserRepository.findByUserAndRoom(user, room)).thenReturn(Optional.empty());

        assertThrows(RoomUserNotFoundException.class,
            () -> roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room));
    }

    // --- getIsUserInActiveRoom ---

    @Test
    void getIsUserInActiveRoom_none_false() {
        User user = userWithId(UUID.randomUUID());
        when(roomUserRepository.findActiveRoomByUser(eq(user), any())).thenReturn(Optional.empty());

        assertFalse(roomsUtilityService.getIsUserInActiveRoom(user));
    }

    @Test
    void getIsUserInActiveRoom_activeAndFresh_true() {
        User user = userWithId(UUID.randomUUID());
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomUserRepository.findActiveRoomByUser(eq(user), any())).thenReturn(Optional.of(room));

        assertTrue(roomsUtilityService.getIsUserInActiveRoom(user));
    }

    @Test
    void getIsUserInActiveRoom_activeButExpired_cancelsAndReturnsFalse() {
        User user = userWithId(UUID.randomUUID());
        Room room = room(RoomStatus.WAITING, LocalDateTime.now().minusSeconds(WAITING_EXPIRATION + 60));
        when(roomUserRepository.findActiveRoomByUser(eq(user), any())).thenReturn(Optional.of(room));
        when(roomInvitationTokenRepository.findRoomInvitations(room)).thenReturn(List.of());

        assertFalse(roomsUtilityService.getIsUserInActiveRoom(user));
        assertEquals(RoomStatus.CANCELLED, room.getStatus());
    }

    @Test
    void throwIsUserInActiveRoom_active_throws() {
        User user = userWithId(UUID.randomUUID());
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomUserRepository.findActiveRoomByUser(eq(user), any())).thenReturn(Optional.of(room));

        assertThrows(UserInActiveRoomException.class, () -> roomsUtilityService.throwIsUserInActiveRoom(user));
    }

    // --- throwIfRoomIsFull ---

    @Test
    void throwIfRoomIsFull_underCapacity_passes() {
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 2);
        when(roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> roomsUtilityService.throwIfRoomIsFull(room));
    }

    @Test
    void throwIfRoomIsFull_atCapacityByPlayers_throws() {
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 4);
        when(roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING))
            .thenReturn(List.of());

        assertThrows(BadActionException.class, () -> roomsUtilityService.throwIfRoomIsFull(room));
    }

    @Test
    void throwIfRoomIsFull_pendingInvitesReserveSeats_throws() {
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 2);
        // 2 players + 2 pending invites == cap of 4.
        when(roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING))
            .thenReturn(List.of(mock(RoomInvitationToken.class), mock(RoomInvitationToken.class)));

        assertThrows(BadActionException.class, () -> roomsUtilityService.throwIfRoomIsFull(room));
    }

    @Test
    void throwIfRoomIsFull_playersPlusInvitesUnderCap_passes() {
        Room room = roomWithGame(gameWithMax(8));
        addPlayers(room, 3);
        when(roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING))
            .thenReturn(List.of(mock(RoomInvitationToken.class)));

        assertDoesNotThrow(() -> roomsUtilityService.throwIfRoomIsFull(room));
    }

    // --- throwIfPlayerLimitReached ---

    @Test
    void throwIfPlayerLimitReached_underCap_passes() {
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 3);

        assertDoesNotThrow(() -> roomsUtilityService.throwIfPlayerLimitReached(room));
    }

    @Test
    void throwIfPlayerLimitReached_atCap_throws() {
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 4);

        assertThrows(BadActionException.class, () -> roomsUtilityService.throwIfPlayerLimitReached(room));
    }

    @Test
    void throwIfPlayerLimitReached_ignoresPendingInvites() {
        // Only joined players count; the accepting invite is still PENDING and
        // must not be double-counted, so 3 players under a cap of 4 passes.
        Room room = roomWithGame(gameWithMax(4));
        addPlayers(room, 3);

        assertDoesNotThrow(() -> roomsUtilityService.throwIfPlayerLimitReached(room));
        verifyNoInteractions(roomInvitationTokenRepository);
    }

    // --- cancelRoom ---

    @Test
    void cancelRoom_setsCancelled_cancelsInvitations_andSession() {
        Room room = room(RoomStatus.WAITING, LocalDateTime.now());
        when(roomInvitationTokenRepository.findRoomInvitations(room)).thenReturn(List.of());

        roomsUtilityService.cancelRoom(room);

        assertEquals(RoomStatus.CANCELLED, room.getStatus());
        verify(roomRepository).save(room);
        verify(sessionUtilitysService).cancelSessionIfExists(room);
    }
}
