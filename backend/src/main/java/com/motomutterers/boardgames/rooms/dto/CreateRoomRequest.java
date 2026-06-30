package com.motomutterers.boardgames.rooms.dto;

import com.motomutterers.boardgames.rooms.model.Room.TrackingMode;   

public class CreateRoomRequest {
    private String gameName;
    private TrackingMode trackingMode;

    public CreateRoomRequest () {}

    public CreateRoomRequest(String gameName, TrackingMode trackingMode){
        this.gameName = gameName;
        this.trackingMode = trackingMode;
    }

    public void setGameName(String gameName){
        this.gameName = gameName;
    }

    public void setTrackingMode(TrackingMode trackingMode){
        this.trackingMode = trackingMode;
    }

    public String getGameName(){
        return gameName;
    }

    public TrackingMode getTrackingMode(){
        return trackingMode;
    }

    @Override
    public String toString() {
        return "CreateRoomRequest{gameName=" + gameName + ", trackingMode=" + trackingMode + "}";
    }
}
