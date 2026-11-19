package com.motomutterers.boardgames.skullking.dto;

public class SubmitBidRequest {
    private String teamId;
    private int bid;
    // The phase event the client was editing when the user tapped. Lets a save that arrives
    // just after the admin advanced still land in the phase it was meant for (see
    // SkullKingService.resolveSubmitTarget). Optional: null means the live phase only.
    private String phaseEventId;

    public SubmitBidRequest(){}

    public SubmitBidRequest(String teamId, int bid){
        this.teamId = teamId;
        this.bid = bid;
    }

    public String getTeamId(){return this.teamId;}
    public int getBid(){return this.bid;}
    public String getPhaseEventId(){return this.phaseEventId;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setBid(int bid){this.bid = bid;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
}
