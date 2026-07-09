package com.motomutterers.boardgames.sessions.events;

import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;

public class TeamSessionEventUpdatedEvent {
    private String roomName;
    private UUID teamId;
    private TeamSessionEvent teamSessionEvent;

    public TeamSessionEventUpdatedEvent(
        String roomName,
        UUID teamId,
        TeamSessionEvent teamSessionEvent
    ) {
        this.roomName = roomName;
        this.teamId = teamId;
        this.teamSessionEvent = teamSessionEvent;
    }

    public String getRoomName(){return this.roomName;}
    public UUID getTeamId(){return this.teamId;}
    public TeamSessionEvent getTeamSessionEvent(){return this.teamSessionEvent;}
}
