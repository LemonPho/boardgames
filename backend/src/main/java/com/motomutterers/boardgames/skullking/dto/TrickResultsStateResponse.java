package com.motomutterers.boardgames.skullking.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class TrickResultsStateResponse extends SkullKingStateResponse {
    private Map<UUID, Integer> bids;
    private Map<UUID, Integer> trickResults;

    public TrickResultsStateResponse(
        SessionEventType gameState,
        int round,
        int cardCount,
        List<TeamResponse> teams,
        Map<UUID, Integer> bids,
        Map<UUID, Integer> trickResults
    ){
        super(gameState, round, cardCount, teams);
        this.bids = bids;
        this.trickResults = trickResults;
    }

    public Map<UUID, Integer> getBids(){return this.bids;}
    public Map<UUID, Integer> getTrickResults(){return this.trickResults;}
}
