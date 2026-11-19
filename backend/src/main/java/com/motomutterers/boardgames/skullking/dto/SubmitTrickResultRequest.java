package com.motomutterers.boardgames.skullking.dto;

public class SubmitTrickResultRequest {
    private String teamId;
    private int tricksWon;
    // The phase event the client was editing when the user tapped. Lets a save that arrives
    // just after the admin advanced still land in the phase it was meant for (see
    // SkullKingService.resolveSubmitTarget). Optional: null means the live phase only.
    private String phaseEventId;

    public SubmitTrickResultRequest(){}

    public SubmitTrickResultRequest(String teamId, int tricksWon){
        this.teamId = teamId;
        this.tricksWon = tricksWon;
    }

    public String getTeamId(){return this.teamId;}
    public int getTricksWon(){return this.tricksWon;}
    public String getPhaseEventId(){return this.phaseEventId;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setTricksWon(int tricksWon){this.tricksWon = tricksWon;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
}
