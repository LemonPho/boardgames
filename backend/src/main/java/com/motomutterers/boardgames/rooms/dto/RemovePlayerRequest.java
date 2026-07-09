package com.motomutterers.boardgames.rooms.dto;

import java.util.UUID;

public class RemovePlayerRequest {
    private UUID roomUserId;
    private String roomName;
    private UUID adminId;

    public RemovePlayerRequest(){}

    public void setRoomUserId(UUID roomUserId){this.roomUserId = roomUserId;}
    public void setRoomName(String roomName){this.roomName = roomName;}
    public void setAdminId(UUID adminId){this.adminId = adminId;}

    public UUID getRoomUserId(){return this.roomUserId;}
    public String getRoomName(){return this.roomName;}
    public UUID getAdminId(){return this.adminId;}
}
