package com.motomutterers.boardgames.skullking.dto;

import java.util.List;

/** Correction of every team's bid for a past round, validated and saved as a set. */
public class CorrectBidsRequest {
    private List<TeamValue> teams;

    public CorrectBidsRequest(){}

    public List<TeamValue> getTeams(){return this.teams;}
    public void setTeams(List<TeamValue> teams){this.teams = teams;}

    public static class TeamValue {
        private String teamId;
        private int value;

        public TeamValue(){}

        public String getTeamId(){return this.teamId;}
        public int getValue(){return this.value;}

        public void setTeamId(String teamId){this.teamId = teamId;}
        public void setValue(int value){this.value = value;}
    }
}
