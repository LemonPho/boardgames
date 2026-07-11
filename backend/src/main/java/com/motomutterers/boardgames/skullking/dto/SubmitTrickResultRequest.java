package com.motomutterers.boardgames.skullking.dto;

public class SubmitTrickResultRequest {
    private String teamId;
    private int tricksWon;

    public SubmitTrickResultRequest(){}

    public SubmitTrickResultRequest(String teamId, int tricksWon){
        this.teamId = teamId;
        this.tricksWon = tricksWon;
    }

    public String getTeamId(){return this.teamId;}
    public int getTricksWon(){return this.tricksWon;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setTricksWon(int tricksWon){this.tricksWon = tricksWon;}
}
