package com.motomutterers.boardgames.skullking.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    // Version of each team's value per field, alongside the values themselves, from the same
    // phase event. Players only receive their own team's entries, exactly like the values. A
    // team with no row has no entry (clients treat it as 0); empty when a phase doesn't apply.
    private Map<UUID, Integer> bidVersions = Map.of();
    private Map<UUID, Integer> trickVersions = Map.of();
    private Map<UUID, Integer> bonusVersions = Map.of();

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
    public Map<UUID, Integer> getBidVersions(){return this.bidVersions;}
    public Map<UUID, Integer> getTrickVersions(){return this.trickVersions;}
    public Map<UUID, Integer> getBonusVersions(){return this.bonusVersions;}

    public void setRoundKey(String roundKey){this.roundKey = roundKey;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
    public void setBidVersions(Map<UUID, Integer> bidVersions){this.bidVersions = bidVersions;}
    public void setTrickVersions(Map<UUID, Integer> trickVersions){this.trickVersions = trickVersions;}
    public void setBonusVersions(Map<UUID, Integer> bonusVersions){this.bonusVersions = bonusVersions;}
}
