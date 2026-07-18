package com.motomutterers.boardgames.notifications.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.notifications.model.NotificationType;

import tools.jackson.databind.ObjectMapper;

/**
 * Discriminated envelope for any notification: the client switches on `type`
 * and reads a fully-typed `data` object, so it never parses a raw payload
 * string or branches on ad-hoc keys.
 *
 * `data` is the deserialized, type-specific payload record (e.g.
 * {@link RoomInvitationPayload} for ROOM_INVITATION). The stored JSON payload
 * already has that exact shape, so we just deserialize it back into the record.
 */
public class NotificationResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID id;
    private NotificationType type;
    private boolean read;
    private LocalDateTime createdAt;
    private Object data;

    public NotificationResponse(){}

    public NotificationResponse(Notification notification){
        this.id = notification.getId();
        this.type = notification.getType();
        this.read = notification.getRead();
        this.createdAt = notification.getCreatedAt();
        this.data = deserializeData(notification.getType(), notification.getPayload());
    }

    // Maps each notification type to its typed payload record. New types add a
    // case here (and a record) — the frontend stays a simple switch on `type`.
    private static Object deserializeData(NotificationType type, String payload){
        if(payload == null) return null;
        return switch(type){
            case ROOM_INVITATION -> MAPPER.readValue(payload, RoomInvitationPayload.class);
        };
    }

    public UUID getId(){return this.id;}
    public NotificationType getType(){return this.type;}
    public boolean getRead(){return this.read;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public Object getData(){return this.data;}

    public void setId(UUID id){this.id = id;}
    public void setType(NotificationType type){this.type = type;}
    public void setRead(boolean read){this.read = read;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public void setData(Object data){this.data = data;}
}
