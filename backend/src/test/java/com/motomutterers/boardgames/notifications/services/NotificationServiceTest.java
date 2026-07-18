package com.motomutterers.boardgames.notifications.services;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.notifications.dto.CreateNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.CreateRoomInvitationNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.NotificationResponse;
import com.motomutterers.boardgames.notifications.dto.RoomInvitationPayload;
import com.motomutterers.boardgames.notifications.events.NotificationCreatedEvent;
import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.notifications.model.NotificationType;
import com.motomutterers.boardgames.notifications.repositories.NotificationRepository;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests notification creation. Uses a real ObjectMapper so the payload is
 * genuinely serialized on write and deserialized back into a typed record by
 * NotificationResponse — the round-trip is the core of the feature.
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            notificationRepository, userService, objectMapper, eventPublisher);
    }

    // helpers
    private User user(String username) {
        User u = new User(username + "@test.com", username, "hash");
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        return u;
    }

    private Room room(String name, String gameName) {
        Game game = new Game();
        game.setName(gameName);
        return new Room(game, name, new RoomConfiguration(TrackingMode.ADMIN, false));
    }

    // --- createRoomInvitationNotification ---

    @Test
    void createRoomInvitation_persistsFlatTypedPayload() {
        User invitee = user("bob");
        User admin = user("alice");
        Room room = room("alice's Skull King Room", "Skull King");
        var request = new CreateRoomInvitationNotificationRequest(invitee, room, admin, "tok-123");

        notificationService.createRoomInvitationNotification(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertEquals(invitee, saved.getUser());
        assertEquals(NotificationType.ROOM_INVITATION, saved.getType());
        assertFalse(saved.getRead());

        // Payload is the flat RoomInvitationPayload shape (not a nested blob).
        RoomInvitationPayload payload = objectMapper.readValue(saved.getPayload(), RoomInvitationPayload.class);
        assertEquals("alice's Skull King Room", payload.roomName());
        assertEquals("Skull King", payload.gameName());
        assertEquals("alice", payload.roomAdminUsername());
        assertEquals("tok-123", payload.token());
    }

    @Test
    void createRoomInvitation_returnsTypedDataInResponse() {
        User invitee = user("bob");
        User admin = user("alice");
        Room room = room("alice's Skull King Room", "Skull King");
        var request = new CreateRoomInvitationNotificationRequest(invitee, room, admin, "tok-123");

        NotificationResponse response = notificationService.createRoomInvitationNotification(request);

        assertEquals(NotificationType.ROOM_INVITATION, response.getType());
        assertFalse(response.getRead());
        // data deserialized back into the typed record.
        assertInstanceOf(RoomInvitationPayload.class, response.getData());
        RoomInvitationPayload data = (RoomInvitationPayload) response.getData();
        assertEquals("alice's Skull King Room", data.roomName());
        assertEquals("tok-123", data.token());
    }

    @Test
    void createRoomInvitation_publishesEventKeyedByRecipientUsername() {
        User invitee = user("bob");
        User admin = user("alice");
        Room room = room("alice's Skull King Room", "Skull King");
        var request = new CreateRoomInvitationNotificationRequest(invitee, room, admin, "tok-123");

        notificationService.createRoomInvitationNotification(request);

        ArgumentCaptor<NotificationCreatedEvent> captor = ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        // Topic is keyed by the invitee's username, not the admin's.
        assertEquals("bob", captor.getValue().getUsername());
        assertEquals(NotificationType.ROOM_INVITATION, captor.getValue().getNotification().getType());
    }

    @Test
    void createRoomInvitation_selfInvite_throwsAndSavesNothing() {
        User self = user("alice");
        Room room = room("alice's Skull King Room", "Skull King");
        // Same user as invitee and admin.
        var request = new CreateRoomInvitationNotificationRequest(self, room, self, "tok-123");

        assertThrows(BadActionException.class,
            () -> notificationService.createRoomInvitationNotification(request));
        verify(notificationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // --- createNotification (generic path) ---

    @Test
    void createNotification_savesAndPublishesEvent() {
        User invitee = user("bob");
        var request = new CreateNotificationRequest(invitee, NotificationType.ROOM_INVITATION, "{}");

        NotificationResponse response = notificationService.createNotification(request);

        verify(notificationRepository).save(any(Notification.class));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
        assertEquals(NotificationType.ROOM_INVITATION, response.getType());
    }
}
