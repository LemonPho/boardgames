package com.motomutterers.boardgames.sessions.dto.teamsessionevent;

import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;

public class CreateTeamSessionEventRequest {
    private String teamId;
    private TeamSessionEventType type;

    public CreateTeamSessionEventRequest(
        String teamId,
        TeamSessionEventType type
    ) {
        this.teamId = teamId;
        this.type = type;
    }

    public String getTeamId(){return this.teamId;}
    public TeamSessionEventType getType(){return this.type;}
}
