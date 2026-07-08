package com.motomutterers.boardgames.sessions.events;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;

public class SessionEventUpdatedEvent {
    private String roomName;
    private SessionEvent sessionEvent;

    public SessionEventUpdatedEvent(
        String roomName,
        SessionEvent sessionEvent
    ) {
        this.roomName = roomName;
        this.sessionEvent = sessionEvent;
    }

    public String getRoomName(){return this.roomName;}
    public SessionEvent getSessionEvent(){return this.sessionEvent;}
}
