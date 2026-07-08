package com.motomutterers.boardgames.notifications.dto;

import com.motomutterers.boardgames.notifications.model.NotificationType;
import com.motomutterers.boardgames.user.model.User;


public class CreateNotificationRequest {
    private User user;
    private NotificationType type;
    private String payload;

    public CreateNotificationRequest(){}

    public CreateNotificationRequest(
        User user,
        NotificationType type,
        String payload
    ) {
        this.user = user;
        this.type = type;
        this.payload = payload;
    }

    public User getUser(){return this.user;}
    public NotificationType getType(){return this.type;}
    public String getPayload(){return this.payload;}

    public void setUser(User user){this.user = user;}
    public void setType(NotificationType type){this.type = type;}
    public void setPayload(String payload){this.payload = payload;}
}
