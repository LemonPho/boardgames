package com.motomutterers.boardgames.teams.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.motomutterers.boardgames.rooms.dto.RoomUserResponse;
import com.motomutterers.boardgames.teams.models.Team;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeamResponse {
    private UUID id;
    private String name;
    private RoomUserResponse player;
    private long finalScore;
    private boolean winner;
    private LocalDateTime createdAt;

    public TeamResponse(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        if (team.getRoomUser() != null) this.player = new RoomUserResponse(team.getRoomUser(), false);
        this.finalScore = team.getFinalScore();
        this.winner = team.getWinner();
        this.createdAt = team.getCreatedAt();
    }

    public TeamResponse(Team team, boolean includePlayer) {
        this.id = team.getId();
        this.name = team.getName();
        if (includePlayer && team.getRoomUser() != null) this.player = new RoomUserResponse(team.getRoomUser(), false);
        this.finalScore = team.getFinalScore();
        this.winner = team.getWinner();
        this.createdAt = team.getCreatedAt();
    }

    public UUID getId(){return this.id;}
    public String getName(){return this.name;}
    public RoomUserResponse getPlayer(){return this.player;}
    public long getFinalScore(){return this.finalScore;}
    public boolean getWinner(){return this.winner;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
}
