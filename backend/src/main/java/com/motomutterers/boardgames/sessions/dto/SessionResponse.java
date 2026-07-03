package com.motomutterers.boardgames.sessions.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.sessions.models.Session;
import com.motomutterers.boardgames.sessions.models.SessionStatus;

public class SessionResponse {
    private String room;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    public SessionResponse(Session session){
        this.room = session.getRoom().getName();
        this.status = session.getStatus();
        this.createdAt = session.getCreatedAt();
        this.endedAt = session.getEndedAt();
    }

    public String getRoom(){return this.room;}
    public SessionStatus getSessionStatus(){return this.status;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public LocalDateTime getEndedAt(){return this.endedAt;}
}
