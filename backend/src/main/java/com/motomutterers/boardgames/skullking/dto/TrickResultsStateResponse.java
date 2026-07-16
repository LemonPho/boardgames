package com.motomutterers.boardgames.skullking.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class TrickResultsStateResponse extends SkullKingStateResponse {
    private Map<UUID, Integer> bids;
    private Map<UUID, Integer> trickResults;
    private boolean krakenPlayed;

    public TrickResultsStateResponse(
        SessionEventType gameState,
        int round,
        int cardCount,
        String startingTeamId,
        List<TeamResponse> teams,
        Map<UUID, Integer> bids,
        Map<UUID, Integer> trickResults,
        boolean krakenPlayed
    ){
        super(gameState, round, cardCount, startingTeamId, teams);
        this.bids = bids;
        this.trickResults = trickResults;
        this.krakenPlayed = krakenPlayed;
    }

    public Map<UUID, Integer> getBids(){return this.bids;}
    public Map<UUID, Integer> getTrickResults(){return this.trickResults;}
    public boolean getKrakenPlayed(){return this.krakenPlayed;}
}
