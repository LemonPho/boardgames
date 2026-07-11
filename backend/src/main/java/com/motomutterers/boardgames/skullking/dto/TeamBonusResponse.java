package com.motomutterers.boardgames.skullking.dto;

public class TeamBonusResponse {
    private int standardFourteens;
    private boolean blackFourteen;
    private int mermaidsByPirate;
    private int piratesBySkullKing;
    private boolean skullKingByMermaid;
    private int loot;

    public TeamBonusResponse(
        int standardFourteens,
        boolean blackFourteen,
        int mermaidsByPirate,
        int piratesBySkullKing,
        boolean skullKingByMermaid,
        int loot
    ){
        this.standardFourteens = standardFourteens;
        this.blackFourteen = blackFourteen;
        this.mermaidsByPirate = mermaidsByPirate;
        this.piratesBySkullKing = piratesBySkullKing;
        this.skullKingByMermaid = skullKingByMermaid;
        this.loot = loot;
    }

    public int getStandardFourteens(){return this.standardFourteens;}
    public boolean getBlackFourteen(){return this.blackFourteen;}
    public int getMermaidsByPirate(){return this.mermaidsByPirate;}
    public int getPiratesBySkullKing(){return this.piratesBySkullKing;}
    public boolean getSkullKingByMermaid(){return this.skullKingByMermaid;}
    public int getLoot(){return this.loot;}
}
