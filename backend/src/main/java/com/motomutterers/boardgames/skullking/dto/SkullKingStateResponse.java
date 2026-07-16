package com.motomutterers.boardgames.skullking.dto;

import java.util.List;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class SkullKingStateResponse {
    private SessionEventType gameState;
    private int round;
    private int cardCount;
    private String startingTeamId;
    private List<TeamResponse> teams;

    public SkullKingStateResponse(
        SessionEventType gameState,
        int round,
        int cardCount,
        String startingTeamId,
        List<TeamResponse> teams
    ){
        this.gameState = gameState;
        this.round = round;
        this.cardCount = cardCount;
        this.startingTeamId = startingTeamId;
        this.teams = teams;
    }

    public SessionEventType getGameState(){return this.gameState;}
    public int getRound(){return this.round;}
    public int getCardCount(){return this.cardCount;}
    public String getStartingTeamId(){return this.startingTeamId;}
    public List<TeamResponse> getTeams(){return this.teams;}
}
