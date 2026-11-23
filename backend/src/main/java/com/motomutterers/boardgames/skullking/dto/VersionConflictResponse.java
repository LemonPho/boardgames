package com.motomutterers.boardgames.skullking.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;

/**
 * 409 body for a stale live save: the team's current value and version, so the client can
 * silently adopt them. Only the fields for `field` are present (NON_NULL drops the rest).
 * With no stored row (version 0) no value fields are sent.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VersionConflictResponse(
    UUID teamId,
    TeamSessionEventType field,
    int version,
    Integer bid,
    Integer tricksWon,
    Integer standardFourteens,
    Boolean blackFourteen,
    Integer mermaidsByPirate,
    Integer piratesBySkullKing,
    Boolean skullKingByMermaid,
    Integer loot
) {
    public static VersionConflictResponse of(UUID teamId, TeamSessionEventType field, int version, TeamSessionEventPayload payload){
        return switch(payload){
            case TeamSessionEventPayload.Bids b ->
                new VersionConflictResponse(teamId, field, version, b.bid(), null, null, null, null, null, null, null);
            case TeamSessionEventPayload.TrickResults t ->
                new VersionConflictResponse(teamId, field, version, null, t.tricksWon(), null, null, null, null, null, null);
            case TeamSessionEventPayload.BonusPoints p ->
                new VersionConflictResponse(teamId, field, version, null, null,
                    p.standardFourteens(), p.blackFourteen(), p.mermaidsByPirate(),
                    p.piratesBySkullKing(), p.skullKingByMermaid(), p.loot());
            case null ->
                new VersionConflictResponse(teamId, field, version, null, null, null, null, null, null, null, null);
        };
    }
}
