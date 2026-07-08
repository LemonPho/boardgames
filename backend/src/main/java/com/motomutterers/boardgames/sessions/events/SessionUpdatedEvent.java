package com.motomutterers.boardgames.sessions.events;

import com.motomutterers.boardgames.sessions.models.session.Session;

public class SessionUpdatedEvent {
    private String roomName;
    private Session session;

    public SessionUpdatedEvent(
        String roomName,
        Session session
    ) {
        this.roomName = roomName;
        this.session = session;
    }

    public String getRoomName(){return this.roomName;}
    public Session getSession(){return this.session;}
}
