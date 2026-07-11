package com.motomutterers.boardgames.skullking.events;

import java.util.UUID;

public class SkullKingStateChangedEvent {
    private String roomName;
    private UUID sessionId;

    public SkullKingStateChangedEvent(String roomName, UUID sessionId){
        this.roomName = roomName;
        this.sessionId = sessionId;
    }

    public String getRoomName(){return this.roomName;}
    public UUID getSessionId(){return this.sessionId;}
}
