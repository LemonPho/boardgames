package com.motomutterers.boardgames.notifications.events;

import java.util.UUID;

/**
 * Published when a notification is marked read (e.g. an invite is accepted).
 * Carries the recipient's username (unique) and the notification id so an
 * AFTER_COMMIT listener can push a dismissal to that user's personal topic,
 * keeping any open client in sync without touching lazy entities.
 */
public class NotificationReadEvent {
    private final String username;
    private final UUID notificationId;

    public NotificationReadEvent(String username, UUID notificationId) {
        this.username = username;
        this.notificationId = notificationId;
    }

    public String getUsername() {
        return this.username;
    }

    public UUID getNotificationId() {
        return this.notificationId;
    }
}
