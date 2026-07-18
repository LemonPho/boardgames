package com.motomutterers.boardgames.notifications.dto;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.user.model.User;

public class CreateRoomInvitationNotificationRequest {
    private User user;
    private Room room;
    private User roomAdmin;
    private String token;

    public CreateRoomInvitationNotificationRequest(){}

    public CreateRoomInvitationNotificationRequest(
        User user,
        Room room,
        User roomAdmin,
        String token
    ) {
        this.user = user;
        this.room = room;
        this.roomAdmin = roomAdmin;
        this.token = token;
    }

    public void setUser(User user){this.user = user;}
    public void setRoom(Room room){this.room = room;}
    public void setRoomAdmin(User roomAdmin){this.roomAdmin = roomAdmin;}
    public void setToken(String token){this.token = token;}

    public User getUser(){return this.user;}
    public Room getRoom(){return this.room;}
    public User getRoomAdmin(){return this.roomAdmin;}
    public String getToken(){return this.token;}
}
