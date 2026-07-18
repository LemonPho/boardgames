package com.motomutterers.boardgames.skullking.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Final standings for a session, keyed by room name. Backs the final scoreboard
 * page — reachable both when a room finishes and from a user's match history.
 * Teams are pre-ranked (1st = highest score).
 */
public class ScoreboardResponse {
    private String roomName;
    private String game;
    private boolean completed;
    private LocalDateTime endedAt;
    private List<ScoreboardTeamResponse> teams;

    public ScoreboardResponse(){}

    public ScoreboardResponse(
        String roomName,
        String game,
        boolean completed,
        LocalDateTime endedAt,
        List<ScoreboardTeamResponse> teams
    ){
        this.roomName = roomName;
        this.game = game;
        this.completed = completed;
        this.endedAt = endedAt;
        this.teams = teams;
    }

    public String getRoomName(){return this.roomName;}
    public String getGame(){return this.game;}
    public boolean getCompleted(){return this.completed;}
    public LocalDateTime getEndedAt(){return this.endedAt;}
    public List<ScoreboardTeamResponse> getTeams(){return this.teams;}

    public static class ScoreboardTeamResponse {
        private String teamId;
        private String playerName;
        private long score;
        private int placement;
        private boolean won;

        public ScoreboardTeamResponse(){}

        public ScoreboardTeamResponse(String teamId, String playerName, long score, int placement, boolean won){
            this.teamId = teamId;
            this.playerName = playerName;
            this.score = score;
            this.placement = placement;
            this.won = won;
        }

        public String getTeamId(){return this.teamId;}
        public String getPlayerName(){return this.playerName;}
        public long getScore(){return this.score;}
        public int getPlacement(){return this.placement;}
        public boolean getWon(){return this.won;}
    }
}
