package com.motomutterers.boardgames.sessions.dto;

import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;

public class SessionStateResponse {
    private SessionResponse session;
    private SessionEventResponse currentEvent;

    public SessionStateResponse(Session session, SessionEvent currentEvent) {
        this.session = new SessionResponse(session);
        if (currentEvent != null) this.currentEvent = new SessionEventResponse(currentEvent);
    }

    public SessionResponse getSession(){return this.session;}
    public SessionEventResponse getCurrentEvent(){return this.currentEvent;}
}
