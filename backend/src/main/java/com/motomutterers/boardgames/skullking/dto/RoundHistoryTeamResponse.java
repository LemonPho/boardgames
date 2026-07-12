package com.motomutterers.boardgames.skullking.dto;

import java.util.UUID;

public class RoundHistoryTeamResponse {
    private UUID teamId;
    private String playerName;
    private Integer bid;
    private Integer tricksWon;
    private TeamBonusResponse bonus;
    private long roundScore;

    public RoundHistoryTeamResponse(
        UUID teamId,
        String playerName,
        Integer bid,
        Integer tricksWon,
        TeamBonusResponse bonus,
        long roundScore
    ){
        this.teamId = teamId;
        this.playerName = playerName;
        this.bid = bid;
        this.tricksWon = tricksWon;
        this.bonus = bonus;
        this.roundScore = roundScore;
    }

    public UUID getTeamId(){return this.teamId;}
    public String getPlayerName(){return this.playerName;}
    public Integer getBid(){return this.bid;}
    public Integer getTricksWon(){return this.tricksWon;}
    public TeamBonusResponse getBonus(){return this.bonus;}
    public long getRoundScore(){return this.roundScore;}
}
