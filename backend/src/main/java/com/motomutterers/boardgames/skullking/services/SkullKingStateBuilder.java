package com.motomutterers.boardgames.skullking.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.sessions.services.SessionEventService;
import com.motomutterers.boardgames.skullking.dto.BidsStateResponse;
import com.motomutterers.boardgames.skullking.dto.BonusPointsStateResponse;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.dto.TeamBonusResponse;
import com.motomutterers.boardgames.skullking.dto.TrickResultsStateResponse;
import com.motomutterers.boardgames.teams.dto.TeamResponse;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;

import tools.jackson.databind.ObjectMapper;

@Service
public class SkullKingStateBuilder {
    private final SessionEventService sessionEventService;
    private final TeamSessionEventRepository teamSessionEventRepository;
    private final ObjectMapper objectMapper;
    private final TeamUtilityService teamUtilityService;

    public SkullKingStateBuilder(
        SessionEventService sessionEventService,
        TeamSessionEventRepository teamSessionEventRepository,
        ObjectMapper objectMapper,
        TeamUtilityService teamUtilityService
    ){
        this.sessionEventService = sessionEventService;
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.objectMapper = objectMapper;
        this.teamUtilityService = teamUtilityService;
    }

    public SkullKingStateResponse buildState(Session session, RoomUser roomUser, boolean isAdmin){
        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);

        // Stable display/turn order by seat position (shared with the game's
        // leader/rotation logic), so the UI and gameplay order always match.
        List<TeamResponse> teams = teamUtilityService.orderedTeams(session).stream()
            .map(TeamResponse::new)
            .toList();

        int round = 1;
        int cardCount = 1;
        String startingTeamId = null;

        switch(currentEvent.getType()){
            case BIDS: {
                SessionEventPayload.Bids payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.Bids.class);
                round = payload.round();
                cardCount = payload.cardCount();
                startingTeamId = payload.startingTeamId();
                break;
            }
            case IN_PROGRESS: {
                SessionEventPayload.InProgress payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.InProgress.class);
                round = payload.round();
                cardCount = payload.cardCount();
                startingTeamId = payload.startingTeamId();
                break;
            }
            case TRICK_RESULTS: {
                SessionEventPayload.TrickResults payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.TrickResults.class);
                round = payload.round();
                cardCount = payload.cardCount();
                startingTeamId = payload.startingTeamId();
                break;
            }
            case BONUS_POINTS: {
                SessionEventPayload.BonusPoints payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.BonusPoints.class);
                round = payload.round();
                cardCount = payload.cardCount();
                startingTeamId = payload.startingTeamId();
                break;
            }
        }

        SessionEvent bidsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS).orElse(null);
        // Bids are secret while bidding; once the round starts they're public (revealed together,
        // as at the table), so every player receives all teams' bids from IN_PROGRESS onward.
        boolean revealBids = isAdmin || currentEvent.getType() != SessionEventType.BIDS;
        Map<UUID, Integer> bids = buildBidsMap(bidsEvent, roomUser, revealBids);
        // Each version map comes from the same phase event as its value map. Versions stay
        // admin-or-own-team even once bids are revealed: players only ever save their own bid.
        // Phases that don't apply keep the response's empty defaults.
        Map<UUID, Integer> bidVersions = buildVersionsMap(bidsEvent, TeamSessionEventType.BIDS, roomUser, isAdmin);
        Map<UUID, Integer> trickVersions = Map.of();
        Map<UUID, Integer> bonusVersions = Map.of();

        SkullKingStateResponse response = switch(currentEvent.getType()){
            case BIDS, IN_PROGRESS -> new BidsStateResponse(currentEvent.getType(), round, cardCount, startingTeamId, teams, bids);
            case TRICK_RESULTS -> {
                Map<UUID, Integer> trickResults = buildTrickResultsMap(currentEvent, roomUser, isAdmin);
                trickVersions = buildVersionsMap(currentEvent, TeamSessionEventType.TRICK_RESULTS, roomUser, isAdmin);
                boolean krakenPlayed = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.TrickResults.class).krakenPlayed();
                yield new TrickResultsStateResponse(currentEvent.getType(), round, cardCount, startingTeamId, teams, bids, trickResults, krakenPlayed);
            }
            case BONUS_POINTS -> {
                SessionEvent trickResultsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS).orElse(null);
                Map<UUID, Integer> trickResults = trickResultsEvent == null
                    ? Map.of()
                    : buildTrickResultsMap(trickResultsEvent, roomUser, isAdmin);
                Map<UUID, TeamBonusResponse> bonuses = buildBonusMap(currentEvent, roomUser, isAdmin);
                trickVersions = buildVersionsMap(trickResultsEvent, TeamSessionEventType.TRICK_RESULTS, roomUser, isAdmin);
                bonusVersions = buildVersionsMap(currentEvent, TeamSessionEventType.BONUS_POINTS, roomUser, isAdmin);
                boolean krakenPlayed = trickResultsEvent != null
                    && objectMapper.readValue(trickResultsEvent.getPayload(), SessionEventPayload.TrickResults.class).krakenPlayed();
                yield new BonusPointsStateResponse(currentEvent.getType(), round, cardCount, startingTeamId, teams, bids, trickResults, bonuses, krakenPlayed);
            }
        };

        // The BIDS event id changes on a new round and on a reset (which recreates it), so
        // clients key round-local drafts on it rather than on the round number.
        response.setRoundKey(bidsEvent == null ? null : bidsEvent.getId().toString());
        // Echoed back on submits so the server can honour the grace window for this phase.
        response.setPhaseEventId(currentEvent.getId().toString());
        // Clients echo these as expectedVersion so a save made on an out-of-date screen is
        // rejected instead of silently overwriting a newer value.
        response.setBidVersions(bidVersions);
        response.setTrickVersions(trickVersions);
        response.setBonusVersions(bonusVersions);
        return response;
    }

    // Same rows and admin-or-own-team filter as the matching value map, mapped to versions.
    private Map<UUID, Integer> buildVersionsMap(SessionEvent phaseEvent, TeamSessionEventType type, RoomUser roomUser, boolean isAdmin){
        if(phaseEvent == null) return Map.of();

        return teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(phaseEvent).stream()
            .filter(event -> event.getType().equals(type))
            .filter(event -> isAdmin || (roomUser.getTeam() != null && event.getTeam().getId().equals(roomUser.getTeam().getId())))
            .collect(Collectors.toMap(
                event -> event.getTeam().getId(),
                TeamSessionEvent::getVersion,
                (a, b) -> b,
                LinkedHashMap::new
            ));
    }

    private Map<UUID, Integer> buildBidsMap(SessionEvent bidsEvent, RoomUser roomUser, boolean showAllTeams){
        if(bidsEvent == null) return Map.of();

        List<TeamSessionEvent> teamEvents = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent);

        return teamEvents.stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BIDS))
            .filter(event -> showAllTeams || (roomUser.getTeam() != null && event.getTeam().getId().equals(roomUser.getTeam().getId())))
            .collect(Collectors.toMap(
                event -> event.getTeam().getId(),
                event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.Bids.class).bid(),
                (a, b) -> b,
                LinkedHashMap::new
            ));
    }

    private Map<UUID, Integer> buildTrickResultsMap(SessionEvent trickResultsEvent, RoomUser roomUser, boolean isAdmin){
        List<TeamSessionEvent> teamEvents = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(trickResultsEvent);

        return teamEvents.stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.TRICK_RESULTS))
            .filter(event -> isAdmin || (roomUser.getTeam() != null && event.getTeam().getId().equals(roomUser.getTeam().getId())))
            .collect(Collectors.toMap(
                event -> event.getTeam().getId(),
                event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.TrickResults.class).tricksWon(),
                (a, b) -> b,
                LinkedHashMap::new
            ));
    }

    private Map<UUID, TeamBonusResponse> buildBonusMap(SessionEvent bonusEvent, RoomUser roomUser, boolean isAdmin){
        List<TeamSessionEvent> teamEvents = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent);

        return teamEvents.stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BONUS_POINTS))
            .filter(event -> isAdmin || (roomUser.getTeam() != null && event.getTeam().getId().equals(roomUser.getTeam().getId())))
            .collect(Collectors.toMap(
                event -> event.getTeam().getId(),
                event -> {
                    TeamSessionEventPayload.BonusPoints payload =
                        objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.BonusPoints.class);
                    return new TeamBonusResponse(
                        payload.standardFourteens(),
                        payload.blackFourteen(),
                        payload.mermaidsByPirate(),
                        payload.piratesBySkullKing(),
                        payload.skullKingByMermaid(),
                        payload.loot()
                    );
                },
                (a, b) -> b,
                LinkedHashMap::new
            ));
    }
}
