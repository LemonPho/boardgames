package com.motomutterers.boardgames.sessions.dto;

public class CreateSessionRequest {
    private String roomName;

    public CreateSessionRequest(String roomName){
        this.roomName = roomName;
    }

    public String getRoomName(){return this.roomName;}

    public void setRoomName(String roomName){this.roomName = roomName;}
}
