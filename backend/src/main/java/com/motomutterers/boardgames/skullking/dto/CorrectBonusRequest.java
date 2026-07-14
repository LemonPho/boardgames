package com.motomutterers.boardgames.skullking.dto;

import java.util.List;

/** Correction of every team's bonus for a past round, validated and saved as a set. */
public class CorrectBonusRequest {
    private List<TeamBonusValue> teams;

    public CorrectBonusRequest(){}

    public List<TeamBonusValue> getTeams(){return this.teams;}
    public void setTeams(List<TeamBonusValue> teams){this.teams = teams;}

    public static class TeamBonusValue {
        private String teamId;
        private int standardFourteens;
        private boolean blackFourteen;
        private int mermaidsByPirate;
        private int piratesBySkullKing;
        private boolean skullKingByMermaid;
        private int loot;

        public TeamBonusValue(){}

        public String getTeamId(){return this.teamId;}
        public int getStandardFourteens(){return this.standardFourteens;}
        public boolean getBlackFourteen(){return this.blackFourteen;}
        public int getMermaidsByPirate(){return this.mermaidsByPirate;}
        public int getPiratesBySkullKing(){return this.piratesBySkullKing;}
        public boolean getSkullKingByMermaid(){return this.skullKingByMermaid;}
        public int getLoot(){return this.loot;}

        public void setTeamId(String teamId){this.teamId = teamId;}
        public void setStandardFourteens(int v){this.standardFourteens = v;}
        public void setBlackFourteen(boolean v){this.blackFourteen = v;}
        public void setMermaidsByPirate(int v){this.mermaidsByPirate = v;}
        public void setPiratesBySkullKing(int v){this.piratesBySkullKing = v;}
        public void setSkullKingByMermaid(boolean v){this.skullKingByMermaid = v;}
        public void setLoot(int v){this.loot = v;}
    }
}
