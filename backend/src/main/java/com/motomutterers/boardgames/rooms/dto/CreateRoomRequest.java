package com.motomutterers.boardgames.rooms.dto;

import com.motomutterers.boardgames.rooms.model.TrackingMode;

public class CreateRoomRequest {
    private String gameName;
    private TrackingMode trackingMode;

    public String getGameName(){
        return gameName;
    }

    public TrackingMode getTrackingMode(){
        return trackingMode;
    }
}
