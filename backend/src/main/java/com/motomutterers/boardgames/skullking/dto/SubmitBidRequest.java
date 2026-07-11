package com.motomutterers.boardgames.skullking.dto;

public class SubmitBidRequest {
    private String teamId;
    private int bid;

    public SubmitBidRequest(){}

    public SubmitBidRequest(String teamId, int bid){
        this.teamId = teamId;
        this.bid = bid;
    }

    public String getTeamId(){return this.teamId;}
    public int getBid(){return this.bid;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setBid(int bid){this.bid = bid;}
}
