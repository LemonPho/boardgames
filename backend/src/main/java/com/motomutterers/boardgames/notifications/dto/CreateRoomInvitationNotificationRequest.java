package com.motomutterers.boardgames.notifications.dto;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.user.model.User;

public class CreateRoomInvitationNotificationRequest {
    private User user;
    private Room room;
    private User roomAdmin;

    public CreateRoomInvitationNotificationRequest(){}

    public CreateRoomInvitationNotificationRequest(
        User user,
        Room room,
        User roomAdmin
    ) {
        this.user = user;
        this.room = room;
        this.roomAdmin = roomAdmin;
    }

    public void setUser(User user){this.user = user;}
    public void setRoom(Room room){this.room = room;}
    public void setRoomAdmin(User roomAdmin){this.roomAdmin = roomAdmin;}

    public User getUser(){return this.user;}
    public Room getRoom(){return this.room;}
    public User getRoomAdmin(){return this.roomAdmin;}
}
