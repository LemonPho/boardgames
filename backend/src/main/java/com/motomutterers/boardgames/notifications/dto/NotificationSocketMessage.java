package com.motomutterers.boardgames.notifications.dto;

import java.util.UUID;

/**
 * Envelope for every push on a user's notification topic. The client switches on
 * {@code event}:
 *   - CREATED : {@code notification} is the new notification to add.
 *   - READ    : {@code id} identifies a notification to dismiss (remove locally).
 * Only the field relevant to the event is populated.
 */
public class NotificationSocketMessage {
    public enum Event { CREATED, READ }

    private Event event;
    private NotificationResponse notification;
    private UUID id;

    public NotificationSocketMessage(){}

    private NotificationSocketMessage(Event event, NotificationResponse notification, UUID id){
        this.event = event;
        this.notification = notification;
        this.id = id;
    }

    public static NotificationSocketMessage created(NotificationResponse notification){
        return new NotificationSocketMessage(Event.CREATED, notification, null);
    }

    public static NotificationSocketMessage read(UUID id){
        return new NotificationSocketMessage(Event.READ, null, id);
    }

    public Event getEvent(){return this.event;}
    public NotificationResponse getNotification(){return this.notification;}
    public UUID getId(){return this.id;}

    public void setEvent(Event event){this.event = event;}
    public void setNotification(NotificationResponse notification){this.notification = notification;}
    public void setId(UUID id){this.id = id;}
}
