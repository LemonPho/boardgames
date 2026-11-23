package com.motomutterers.boardgames.skullking.dto;

public class SubmitBonusPointsRequest {
    private String teamId;
    private int standardFourteens;
    private boolean blackFourteen;
    private int mermaidsByPirate;
    private int piratesBySkullKing;
    private boolean skullKingByMermaid;
    private int loot;
    // The phase event the client was editing when the user tapped. Lets a save that arrives
    // just after the admin advanced still land in the phase it was meant for (see
    // SkullKingService.resolveSubmitTarget). Optional: null means the live phase only.
    private String phaseEventId;
    // The version of this team's value the client last saw. A save made against an older
    // version is rejected with 409 and the current value (see VersionConflictException).
    // Optional: null skips the check (legacy clients and tools).
    private Integer expectedVersion;

    public SubmitBonusPointsRequest(){}

    public String getTeamId(){return this.teamId;}
    public int getStandardFourteens(){return this.standardFourteens;}
    public boolean getBlackFourteen(){return this.blackFourteen;}
    public int getMermaidsByPirate(){return this.mermaidsByPirate;}
    public int getPiratesBySkullKing(){return this.piratesBySkullKing;}
    public boolean getSkullKingByMermaid(){return this.skullKingByMermaid;}
    public int getLoot(){return this.loot;}
    public String getPhaseEventId(){return this.phaseEventId;}
    public Integer getExpectedVersion(){return this.expectedVersion;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setStandardFourteens(int standardFourteens){this.standardFourteens = standardFourteens;}
    public void setBlackFourteen(boolean blackFourteen){this.blackFourteen = blackFourteen;}
    public void setMermaidsByPirate(int mermaidsByPirate){this.mermaidsByPirate = mermaidsByPirate;}
    public void setPiratesBySkullKing(int piratesBySkullKing){this.piratesBySkullKing = piratesBySkullKing;}
    public void setSkullKingByMermaid(boolean skullKingByMermaid){this.skullKingByMermaid = skullKingByMermaid;}
    public void setLoot(int loot){this.loot = loot;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
    public void setExpectedVersion(Integer expectedVersion){this.expectedVersion = expectedVersion;}
}
