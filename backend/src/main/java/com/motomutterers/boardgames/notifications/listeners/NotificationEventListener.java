package com.motomutterers.boardgames.notifications.listeners;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.notifications.dto.NotificationSocketMessage;
import com.motomutterers.boardgames.notifications.events.NotificationCreatedEvent;
import com.motomutterers.boardgames.notifications.events.NotificationReadEvent;

/**
 * Pushes notification changes to the recipient's personal topic once the
 * surrounding transaction commits, so an open client stays in sync without
 * polling: a CREATED envelope when an invite arrives, a READ envelope when one
 * is dismissed (e.g. accepted). Mirrors RoomEventListener's AFTER_COMMIT
 * broadcast pattern.
 */
@Component
public class NotificationEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private String topicFor(String username){
        return "/topic/notifications/" + username;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSend(
            topicFor(event.getUsername()),
            NotificationSocketMessage.created(event.getNotification()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationRead(NotificationReadEvent event) {
        messagingTemplate.convertAndSend(
            topicFor(event.getUsername()),
            NotificationSocketMessage.read(event.getNotificationId()));
    }
}
