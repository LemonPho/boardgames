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

    public SubmitBonusPointsRequest(){}

    public String getTeamId(){return this.teamId;}
    public int getStandardFourteens(){return this.standardFourteens;}
    public boolean getBlackFourteen(){return this.blackFourteen;}
    public int getMermaidsByPirate(){return this.mermaidsByPirate;}
    public int getPiratesBySkullKing(){return this.piratesBySkullKing;}
    public boolean getSkullKingByMermaid(){return this.skullKingByMermaid;}
    public int getLoot(){return this.loot;}
    public String getPhaseEventId(){return this.phaseEventId;}

    public void setTeamId(String teamId){this.teamId = teamId;}
    public void setStandardFourteens(int standardFourteens){this.standardFourteens = standardFourteens;}
    public void setBlackFourteen(boolean blackFourteen){this.blackFourteen = blackFourteen;}
    public void setMermaidsByPirate(int mermaidsByPirate){this.mermaidsByPirate = mermaidsByPirate;}
    public void setPiratesBySkullKing(int piratesBySkullKing){this.piratesBySkullKing = piratesBySkullKing;}
    public void setSkullKingByMermaid(boolean skullKingByMermaid){this.skullKingByMermaid = skullKingByMermaid;}
    public void setLoot(int loot){this.loot = loot;}
    public void setPhaseEventId(String phaseEventId){this.phaseEventId = phaseEventId;}
}
