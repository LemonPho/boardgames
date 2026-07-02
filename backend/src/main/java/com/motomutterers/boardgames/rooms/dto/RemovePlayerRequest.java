package com.motomutterers.boardgames.rooms.dto;

import java.util.UUID;

public class RemovePlayerRequest {
    private String displayName;
    private String roomName;
    private UUID adminId;

    public RemovePlayerRequest(){}

    public RemovePlayerRequest(
        String displayName
    ) {
        this.displayName = displayName;
    }

    public void setDisplayName(String displayName){this.displayName = displayName;}
    public void setRoomName(String roomName){this.roomName = roomName;}
    public void setAdminId(UUID adminId){this.adminId = adminId;}

    public String getDisplayName(){return this.displayName;}
    public String getRoomName(){return this.roomName;}
    public UUID getAdminId(){return this.adminId;}
}
