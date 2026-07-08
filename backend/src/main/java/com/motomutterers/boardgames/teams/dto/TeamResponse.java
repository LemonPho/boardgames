package com.motomutterers.boardgames.teams.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.motomutterers.boardgames.teams.models.Team;

public class TeamResponse {
    private String name;
    private List<TeamUserResponse> teamUsers;
    private long finalScore = 0;
    private boolean winner = false;
    private LocalDateTime createdAt;

    public TeamResponse(
        Team team
    ) {
        this.name = team.getName();
        this.teamUsers = team.getTeamUsers().stream()
            .map(user -> new TeamUserResponse(user))
            .collect(Collectors.toList());
    }

    public String getName(){return this.name;}
    public List<TeamUserResponse> getTeamUsers(){return this.teamUsers;}
    public long getFinalScore(){return this.finalScore;}
    public boolean getWinner(){return this.winner;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
}
