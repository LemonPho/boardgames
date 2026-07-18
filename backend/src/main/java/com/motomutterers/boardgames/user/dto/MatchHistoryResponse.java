package com.motomutterers.boardgames.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One completed match from a user's perspective, for the profile match history.
 * Placement is 1-based rank within the match (by final score); won is placement
 * 1. Score is the user's team final score.
 */
public class MatchHistoryResponse {
    private UUID sessionId;
    private String game;
    private String roomName;
    private int placement;
    private int players;
    private long score;
    private boolean won;
    private LocalDateTime playedAt;

    public MatchHistoryResponse(){}

    public MatchHistoryResponse(
        UUID sessionId,
        String game,
        String roomName,
        int placement,
        int players,
        long score,
        boolean won,
        LocalDateTime playedAt
    ){
        this.sessionId = sessionId;
        this.game = game;
        this.roomName = roomName;
        this.placement = placement;
        this.players = players;
        this.score = score;
        this.won = won;
        this.playedAt = playedAt;
    }

    public UUID getSessionId(){return this.sessionId;}
    public String getGame(){return this.game;}
    public String getRoomName(){return this.roomName;}
    public int getPlacement(){return this.placement;}
    public int getPlayers(){return this.players;}
    public long getScore(){return this.score;}
    public boolean getWon(){return this.won;}
    public LocalDateTime getPlayedAt(){return this.playedAt;}
}
