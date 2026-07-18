package com.motomutterers.boardgames.notifications.events;

import com.motomutterers.boardgames.notifications.dto.NotificationResponse;

/**
 * Published when a notification is created. Carries the recipient's username
 * (unique) and the fully-built response so an AFTER_COMMIT listener can push it
 * over WebSocket to that user's personal topic without touching lazy entities.
 */
public class NotificationCreatedEvent {
    private final String username;
    private final NotificationResponse notification;

    public NotificationCreatedEvent(String username, NotificationResponse notification) {
        this.username = username;
        this.notification = notification;
    }

    public String getUsername() {
        return this.username;
    }

    public NotificationResponse getNotification() {
        return this.notification;
    }
}
