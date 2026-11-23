package com.motomutterers.boardgames.skullking.dto;

public class SubmitTrickResultRequest {
    private String teamId;
    private int tricksWon;
    // The phase event the client was editing when the user tapped. Lets a save that arrives
    // just after the admin advanced still land in the phase it was meant for (see
    // SkullKingService.resolveSubmitTarget). Optional: null means the live phase only.
    private String phaseEventId;
    // The version of this team's value the client last saw. A save made against an older
    // version is rejected with 409 and the current value (see VersionConflictException).
    // Optional: null skips the check (legacy clients and tools).
    private Integer expectedVersion;

    public SubmitTrickResultRequest(){}

    public SubmitTrickResultRequest(String teamId, int tricksWon){
        this.teamId = teamId;
        this.tricksWon = tricksWon;
    }

    public String getTeamId(){return this.teamId;}
    public int getTricksWon(){return this.tricksWon;}
    public String getPhaseEventId(){return this.phaseEventId;}
    public Integer getExpectedVersion(){return this.expectedVersion;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setTricksWon(int tricksWon){this.tricksWon = tricksWon;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
    public void setExpectedVersion(Integer expectedVersion){this.expectedVersion = expectedVersion;}
}
