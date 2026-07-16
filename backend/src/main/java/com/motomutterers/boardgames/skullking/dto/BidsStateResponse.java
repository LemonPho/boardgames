package com.motomutterers.boardgames.skullking.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.teams.dto.TeamResponse;

public class BidsStateResponse extends SkullKingStateResponse {
    private Map<UUID, Integer> bids;

    public BidsStateResponse(
        SessionEventType gameState,
        int round,
        int cardCount,
        String startingTeamId,
        List<TeamResponse> teams,
        Map<UUID, Integer> bids
    ){
        super(gameState, round, cardCount, startingTeamId, teams);
        this.bids = bids;
    }

    public Map<UUID, Integer> getBids(){return this.bids;}
}
