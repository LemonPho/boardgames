package com.motomutterers.boardgames.skullking.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class BonusPointsStateResponse extends SkullKingStateResponse {
    private Map<UUID, Integer> bids;
    private Map<UUID, Integer> trickResults;
    private Map<UUID, TeamBonusResponse> bonuses;

    public BonusPointsStateResponse(
        SessionEventType gameState,
        int round,
        int cardCount,
        List<TeamResponse> teams,
        Map<UUID, Integer> bids,
        Map<UUID, Integer> trickResults,
        Map<UUID, TeamBonusResponse> bonuses
    ){
        super(gameState, round, cardCount, teams);
        this.bids = bids;
        this.trickResults = trickResults;
        this.bonuses = bonuses;
    }

    public Map<UUID, Integer> getBids(){return this.bids;}
    public Map<UUID, Integer> getTrickResults(){return this.trickResults;}
    public Map<UUID, TeamBonusResponse> getBonuses(){return this.bonuses;}
}
