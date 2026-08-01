package com.motomutterers.boardgames.notifications.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.notifications.dto.CreateNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.CreateRoomInvitationNotificationRequest;
import com.motomutterers.boardgames.notifications.dto.NotificationResponse;
import com.motomutterers.boardgames.notifications.dto.RoomInvitationPayload;
import com.motomutterers.boardgames.notifications.events.NotificationCreatedEvent;
import com.motomutterers.boardgames.notifications.events.NotificationReadEvent;
import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.notifications.model.NotificationType;
import com.motomutterers.boardgames.notifications.repositories.NotificationRepository;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import tools.jackson.databind.ObjectMapper;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserService userService,
        ObjectMapper objectMapper,
        ApplicationEventPublisher eventPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void readNotification(UUID notificationId, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new BadActionException("Notification not found"));

        // A user can only mark their own notifications as read.
        if(!notification.getUser().getId().equals(user.getId())){
            throw new UnauthorizedException("You can only update your own notifications");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // Dismiss the invitation notification for a token once the invite is accepted,
    // so it stops showing as unread. Scoped to the token, so it only clears the
    // one invite that was acted on. Publishes a read event per notification so an
    // open client drops it live (see NotificationEventListener).
    @Transactional
    public void markInvitationRead(String token){
        for(Notification notification : notificationRepository.findUnreadByInvitationToken(token)){
            notification.setRead(true);
            notificationRepository.save(notification);
            eventPublisher.publishEvent(
                new NotificationReadEvent(notification.getUser().getUsername(), notification.getId()));
        }
    }

    // Delete the invitation notification for a token once the invite is revoked —
    // a revoked invite is gone, not just dismissed, so the notification is removed
    // entirely. Scoped to the token, so it only clears the one revoked invite.
    // Publishes a read event per notification so an open client drops it live.
    @Transactional
    public void deleteInvitation(String token){
        List<Notification> notifications = notificationRepository.findByInvitationToken(token);
        for(Notification notification : notifications){
            eventPublisher.publishEvent(
                new NotificationReadEvent(notification.getUser().getUsername(), notification.getId()));
        }
        notificationRepository.deleteAll(notifications);
    }

    public List<NotificationResponse> getNotifications(Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        List<NotificationResponse> notifications = notificationRepository.findNotificationsByUserAndReadFalse(user)
            .stream()
            .map(NotificationResponse::new)
            .collect(Collectors.toList());
        return notifications;
    }

    public NotificationResponse createRoomInvitationNotification(CreateRoomInvitationNotificationRequest request){
        if(request.getUser().getId().equals(request.getRoomAdmin().getId())){
            throw new BadActionException("You cannot send a notification to yourself");
        }

        // Persist the flat, typed payload — the same shape sent to the client — so
        // the notification carries everything needed to render and accept the invite.
        RoomInvitationPayload payloadRecord = new RoomInvitationPayload(
            request.getRoom().getName(),
            request.getRoom().getGame().getName(),
            request.getRoomAdmin().getUsername(),
            request.getToken());
        String payload = objectMapper.writeValueAsString(payloadRecord);

        CreateNotificationRequest createNotificationRequest = new CreateNotificationRequest(
            request.getUser(),
            NotificationType.ROOM_INVITATION,
            payload);

        return createNotification(createNotificationRequest);
    }

    public NotificationResponse createNotification(CreateNotificationRequest request){
        Notification notification = new Notification(request.getUser(), request.getType(), request.getPayload());
        notificationRepository.save(notification);
        NotificationResponse response = new NotificationResponse(notification);

        // Pushed to the recipient's WebSocket topic after the surrounding
        // transaction commits (see NotificationEventListener). Keyed by username,
        // which is unique.
        eventPublisher.publishEvent(new NotificationCreatedEvent(request.getUser().getUsername(), response));

        return response;
    }
}
