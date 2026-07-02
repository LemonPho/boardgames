package com.motomutterers.boardgames.notifications.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.notifications.model.Notification;
import com.motomutterers.boardgames.notifications.model.NotificationType;
import com.motomutterers.boardgames.user.dto.UserResponse;

public class NotificationResponse {
    private UserResponse user;
    private NotificationType type;
    private String payload;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationResponse(){}

    public NotificationResponse(
        Notification notification
    ){
        this.user = new UserResponse(notification.getUser());
        this.type = notification.getType();
        this.payload = notification.getPayload();
        this.read = notification.getRead();
        this.createdAt = notification.getCreatedAt(); 
    }

    public UserResponse getUserResponse(){return this.user;}
    public NotificationType getType(){return this.type;}
    public String getPayload(){return this.payload;}
    public boolean getRead(){return this.read;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setUserResponse(UserResponse user){this.user = user;}
    public void setNotificationType(NotificationType type){this.type = type;}
    public void setPayload(String payload){this.payload = payload;}
    public void setRead(boolean read){this.read = read;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}

}
