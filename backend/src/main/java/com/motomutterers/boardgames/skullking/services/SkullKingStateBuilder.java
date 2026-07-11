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

import tools.jackson.databind.ObjectMapper;

@Service
public class SkullKingStateBuilder {
    private final SessionEventService sessionEventService;
    private final TeamSessionEventRepository teamSessionEventRepository;
    private final ObjectMapper objectMapper;

    public SkullKingStateBuilder(
        SessionEventService sessionEventService,
        TeamSessionEventRepository teamSessionEventRepository,
        ObjectMapper objectMapper
    ){
        this.sessionEventService = sessionEventService;
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.objectMapper = objectMapper;
    }

    public SkullKingStateResponse buildState(Session session, RoomUser roomUser, boolean isAdmin){
        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        List<TeamResponse> teams = session.getTeams().stream().map(TeamResponse::new).toList();

        int round = 1;
        int cardCount = 1;

        switch(currentEvent.getType()){
            case BIDS: {
                SessionEventPayload.Bids payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.Bids.class);
                round = payload.round();
                cardCount = payload.cardCount();
                break;
            }
            case IN_PROGRESS: {
                SessionEventPayload.InProgress payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.InProgress.class);
                round = payload.round();
                cardCount = payload.cardCount();
                break;
            }
            case TRICK_RESULTS: {
                SessionEventPayload.TrickResults payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.TrickResults.class);
                round = payload.round();
                cardCount = payload.cardCount();
                break;
            }
            case BONUS_POINTS: {
                SessionEventPayload.BonusPoints payload = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.BonusPoints.class);
                round = payload.round();
                cardCount = payload.cardCount();
                break;
            }
        }

        SessionEvent bidsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS).orElse(null);
        Map<UUID, Integer> bids = buildBidsMap(bidsEvent, roomUser, isAdmin);

        switch(currentEvent.getType()){
            case BIDS:
                return new BidsStateResponse(currentEvent.getType(), round, cardCount, teams, bids);
            case IN_PROGRESS:
                return new BidsStateResponse(currentEvent.getType(), round, cardCount, teams, bids);
            case TRICK_RESULTS: {
                Map<UUID, Integer> trickResults = buildTrickResultsMap(currentEvent, roomUser, isAdmin);
                return new TrickResultsStateResponse(currentEvent.getType(), round, cardCount, teams, bids, trickResults);
            }
            case BONUS_POINTS: {
                SessionEvent trickResultsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS).orElse(null);
                Map<UUID, Integer> trickResults = trickResultsEvent == null
                    ? Map.of()
                    : buildTrickResultsMap(trickResultsEvent, roomUser, isAdmin);
                Map<UUID, TeamBonusResponse> bonuses = buildBonusMap(currentEvent, roomUser, isAdmin);
                return new BonusPointsStateResponse(currentEvent.getType(), round, cardCount, teams, bids, trickResults, bonuses);
            }
            default:
                return new SkullKingStateResponse(currentEvent.getType(), round, cardCount, teams);
        }
    }

    private Map<UUID, Integer> buildBidsMap(SessionEvent bidsEvent, RoomUser roomUser, boolean isAdmin){
        if(bidsEvent == null) return Map.of();

        List<TeamSessionEvent> teamEvents = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bidsEvent);

        return teamEvents.stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BIDS))
            .filter(event -> isAdmin || (roomUser.getTeam() != null && event.getTeam().getId().equals(roomUser.getTeam().getId())))
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
