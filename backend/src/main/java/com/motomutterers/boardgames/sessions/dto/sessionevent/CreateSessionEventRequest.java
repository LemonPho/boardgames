package com.motomutterers.boardgames.sessions.dto.sessionevent;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;

public class CreateSessionEventRequest {
    private String roomName;
    private int sequence;
    private SessionEventType type;

    public CreateSessionEventRequest(){}

    public CreateSessionEventRequest(
        String roomName,
        int sequence,
        SessionEventType type
    ) {
        this.roomName = roomName;
        this.sequence = sequence;
        this.type = type;
    }

    public void setRoomName(String roomName){this.roomName = roomName;}
    public void setSequence(int sequence){this.sequence = sequence;}
    public void setType(SessionEventType type){this.type = type;}

    public String getRoomName(){return this.roomName;}
    public int getSequence(){return this.sequence;}
    public SessionEventType getType(){return this.type;}
}
