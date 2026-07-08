package com.motomutterers.boardgames.sessions.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.session.SessionStatus;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class SessionResponse {
    private String room;
    private List<TeamResponse> teams;
    private SessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    public SessionResponse(Session session){
        this.room = session.getRoom().getName();
        this.teams = session.getTeams().stream()
            .map(TeamResponse::new)
            .collect(Collectors.toList());
        this.status = session.getStatus();
        this.createdAt = session.getCreatedAt();
        this.endedAt = session.getEndedAt();
    }

    public String getRoom(){return this.room;}
    public List<TeamResponse> getTeams(){return this.teams;}
    public SessionStatus getSessionStatus(){return this.status;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public LocalDateTime getEndedAt(){return this.endedAt;}
}
