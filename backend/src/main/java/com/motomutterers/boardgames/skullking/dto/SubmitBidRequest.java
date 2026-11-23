package com.motomutterers.boardgames.skullking.dto;

public class SubmitBidRequest {
    private String teamId;
    private int bid;
    // The phase event the client was editing when the user tapped. Lets a save that arrives
    // just after the admin advanced still land in the phase it was meant for (see
    // SkullKingService.resolveSubmitTarget). Optional: null means the live phase only.
    private String phaseEventId;
    // The version of this team's value the client last saw. A save made against an older
    // version is rejected with 409 and the current value (see VersionConflictException).
    // Optional: null skips the check (legacy clients and tools).
    private Integer expectedVersion;

    public SubmitBidRequest(){}

    public SubmitBidRequest(String teamId, int bid){
        this.teamId = teamId;
        this.bid = bid;
    }

    public String getTeamId(){return this.teamId;}
    public int getBid(){return this.bid;}
    public String getPhaseEventId(){return this.phaseEventId;}
    public Integer getExpectedVersion(){return this.expectedVersion;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setBid(int bid){this.bid = bid;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
    public void setExpectedVersion(Integer expectedVersion){this.expectedVersion = expectedVersion;}
}
