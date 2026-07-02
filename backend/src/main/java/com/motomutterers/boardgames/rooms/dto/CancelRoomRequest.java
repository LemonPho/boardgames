package com.motomutterers.boardgames.rooms.dto;

public class CancelRoomRequest {
    private String roomName;

    public CancelRoomRequest(){}

    public CancelRoomRequest(String roomName){
        this.roomName = roomName;
    }

    public String getRoomName(){return this.roomName;}

    public void setRoomName(String roomName){this.roomName = roomName;}
}
