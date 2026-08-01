package com.motomutterers.boardgames.rooms.dto;

import java.util.UUID;

/**
 * Admin action to move a player to a new seat/turn position. The moved player
 * lands at {@code newLocation} (0-based) and everyone in between shifts by one to
 * keep positions contiguous. adminId/roomName are set server-side from the path +
 * auth, so the client only sends roomUserId + newLocation.
 */
public class MovePlayerRequest {
    private UUID roomUserId;
    private int newLocation;
    private String roomName;
    private UUID adminId;

    public MovePlayerRequest(){}

    public void setRoomUserId(UUID roomUserId){this.roomUserId = roomUserId;}
    public void setNewLocation(int newLocation){this.newLocation = newLocation;}
    public void setRoomName(String roomName){this.roomName = roomName;}
    public void setAdminId(UUID adminId){this.adminId = adminId;}

    public UUID getRoomUserId(){return this.roomUserId;}
    public int getNewLocation(){return this.newLocation;}
    public String getRoomName(){return this.roomName;}
    public UUID getAdminId(){return this.adminId;}
}
