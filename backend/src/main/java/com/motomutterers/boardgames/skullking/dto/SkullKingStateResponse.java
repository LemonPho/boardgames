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
    // Identity of the current round's BIDS event: changes on a new round AND on a reset, so
    // clients can drop round-local drafts.
    private String roundKey;
    // Id of the current phase event. Clients echo it on submits so a save that reaches the
    // server just after the admin advanced is still applied to the phase it was made in.
    private String phaseEventId;

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
    public String getRoundKey(){return this.roundKey;}
    public String getPhaseEventId(){return this.phaseEventId;}

    public void setRoundKey(String roundKey){this.roundKey = roundKey;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
}
