package com.motomutterers.boardgames.skullking.dto;

import java.util.List;

public class RoundHistoryResponse {
    private int round;
    private int cardCount;
    private boolean completed;
    private boolean krakenPlayed;
    private List<RoundHistoryTeamResponse> teams;

    public RoundHistoryResponse(
        int round,
        int cardCount,
        boolean completed,
        boolean krakenPlayed,
        List<RoundHistoryTeamResponse> teams
    ){
        this.round = round;
        this.cardCount = cardCount;
        this.completed = completed;
        this.krakenPlayed = krakenPlayed;
        this.teams = teams;
    }

    public int getRound(){return this.round;}
    public int getCardCount(){return this.cardCount;}
    public boolean getCompleted(){return this.completed;}
    public boolean getKrakenPlayed(){return this.krakenPlayed;}
    public List<RoundHistoryTeamResponse> getTeams(){return this.teams;}
}
