package com.motomutterers.boardgames.rooms.dto;

import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;

public class CreateRoomRequest {
    private String gameName;
    private RoomConfiguration configuration;

    public CreateRoomRequest () {}

    public CreateRoomRequest(String gameName, RoomConfiguration configuration){
        this.gameName = gameName;
        this.configuration = configuration;
    }

    public void setGameName(String gameName){
        this.gameName = gameName;
    }

    public void setConfiguration(RoomConfiguration configuration){
        this.configuration = configuration;
    }

    public String getGameName(){
        return gameName;
    }

    public RoomConfiguration getConfiguration(){
        return configuration;
    }

    @Override
    public String toString() {
        return "CreateRoomRequest{gameName=" + gameName + ", configuration=" + configuration + "}";
    }
}
