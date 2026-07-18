package com.motomutterers.boardgames.notifications.listeners;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.notifications.events.NotificationCreatedEvent;

/**
 * Pushes newly-created notifications to the recipient's personal topic once the
 * creating transaction commits, so an invited user sees the notification live
 * without polling. Mirrors RoomEventListener's AFTER_COMMIT broadcast pattern.
 */
@Component
public class NotificationEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/notifications/" + event.getUsername(),
            event.getNotification());
    }
}
