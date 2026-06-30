package com.motomutterers.boardgames.rooms.dto;

public class RoomInvitationRequest {
    private String username;
    private String roomName;

    public RoomInvitationRequest(){}

    public RoomInvitationRequest(
        String username,
        String roomName
    ) {
        this.username = username;
        this.roomName = roomName;
    }

    public void setUsername(String username){this.username = username;}
    public void setRoomName(String roomName){this.roomName = roomName;}

    public String getUsername(){return this.username;}
    public String getRoomName(){return this.roomName;}
}
