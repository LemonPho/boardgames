package com.motomutterers.boardgames.skullking.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventPayload;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.sessions.services.SessionEventService;
import com.motomutterers.boardgames.sessions.services.SessionUtilitysService;
import com.motomutterers.boardgames.sessions.services.TeamSessionEventService;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.dto.SubmitBidRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitBonusPointsRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitTrickResultRequest;
import com.motomutterers.boardgames.skullking.events.SkullKingStateChangedEvent;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class SkullKingService {
    private static final int MAX_ROUNDS = 10;

    private final UserService userService;
    private final RoomsUtilityService roomsUtilityService;
    private final SessionUtilitysService sessionUtilitysService;
    private final TeamUtilityService teamUtilityService;
    private final SessionEventService sessionEventService;
    private final TeamSessionEventService teamSessionEventService;
    private final TeamSessionEventRepository teamSessionEventRepository;
    private final SkullKingStateBuilder stateBuilder;
    private final ObjectMapper objectMapper;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    public SkullKingService(
        UserService userService,
        RoomsUtilityService roomsUtilityService,
        SessionUtilitysService sessionUtilitysService,
        TeamUtilityService teamUtilityService,
        SessionEventService sessionEventService,
        TeamSessionEventService teamSessionEventService,
        TeamSessionEventRepository teamSessionEventRepository,
        SkullKingStateBuilder stateBuilder,
        ObjectMapper objectMapper
    ){
        this.userService = userService;
        this.roomsUtilityService = roomsUtilityService;
        this.sessionUtilitysService = sessionUtilitysService;
        this.teamUtilityService = teamUtilityService;
        this.sessionEventService = sessionEventService;
        this.teamSessionEventService = teamSessionEventService;
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.stateBuilder = stateBuilder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void createInitialRound(Session session){
        sessionEventService.createSessionEvent(session, SessionEventType.BIDS, new SessionEventPayload.Bids(1, 1));
    }

    @Transactional
    public SkullKingStateResponse getState(String roomName, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);
        boolean isAdmin = roomUser.getRole().equals(RoomUserRoles.ADMIN);

        // Loading or reconnecting to the game keeps the room alive so an active
        // in-person game isn't expired between server actions.
        roomsUtilityService.updateRoomLastUpdated(room);

        return stateBuilder.buildState(session, roomUser, isAdmin);
    }

    @Transactional
    public void submitBid(String roomName, SubmitBidRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);
        Team team = teamUtilityService.getOrThrowTeamById(request.getTeamId());

        throwIfUserCantSubmitForTeam(room, roomUser, team);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.BIDS)){
            throw new BadActionException("Bids can only be submitted during the bidding phase");
        }

        SessionEventPayload.Bids bidsPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.Bids.class);
        if(request.getBid() < 0 || request.getBid() > bidsPhase.cardCount()){
            throw new BadActionException("Bid must be between 0 and " + bidsPhase.cardCount());
        }

        teamSessionEventService.upsertTeamSessionEvent(
            session,
            currentEvent,
            team,
            TeamSessionEventType.BIDS,
            new TeamSessionEventPayload.Bids(request.getBid()));

        publishStateChanged(room, session);
    }

    @Transactional
    public void submitTrickResult(String roomName, SubmitTrickResultRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);
        Team team = teamUtilityService.getOrThrowTeamById(request.getTeamId());

        throwIfUserCantSubmitForTeam(room, roomUser, team);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.TRICK_RESULTS)){
            throw new BadActionException("Trick results can only be submitted during the trick results phase");
        }

        SessionEventPayload.TrickResults trickResultsPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.TrickResults.class);
        int cardCount = trickResultsPhase.cardCount();
        if(request.getTricksWon() < 0 || request.getTricksWon() > cardCount){
            throw new BadActionException("Tricks won must be between 0 and " + cardCount);
        }

        // Exclude this team's own existing value: a correction replaces it rather
        // than adding to the running total.
        int tricksByOthers = sumTricksWon(currentEvent) - teamTricksWon(currentEvent, team);
        if(tricksByOthers + request.getTricksWon() > cardCount){
            throw new BadActionException("Total tricks won cannot exceed the " + cardCount + " cards dealt this round");
        }

        teamSessionEventService.upsertTeamSessionEvent(
            session,
            currentEvent,
            team,
            TeamSessionEventType.TRICK_RESULTS,
            new TeamSessionEventPayload.TrickResults(request.getTricksWon()));

        publishStateChanged(room, session);
    }

    @Transactional
    public void startRound(String roomName, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.BIDS)){
            throw new BadActionException("A round can only be started from the bidding phase");
        }

        int submittedBids = teamSessionEventRepository.countBySessionEvent(currentEvent);
        if(submittedBids < session.getTeams().size()){
            throw new BadActionException("All teams must submit their bids before the round can start");
        }

        SessionEventPayload.Bids bidsPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.Bids.class);
        sessionEventService.createSessionEvent(
            session,
            SessionEventType.IN_PROGRESS,
            new SessionEventPayload.InProgress(bidsPhase.round(), bidsPhase.cardCount()));

        publishStateChanged(room, session);
    }

    @Transactional
    public void startTrickResults(String roomName, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.IN_PROGRESS)){
            throw new BadActionException("Trick results can only be entered while a round is in progress");
        }

        SessionEventPayload.InProgress inProgressPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.InProgress.class);
        sessionEventService.createSessionEvent(
            session,
            SessionEventType.TRICK_RESULTS,
            new SessionEventPayload.TrickResults(inProgressPhase.round(), inProgressPhase.cardCount()));

        publishStateChanged(room, session);
    }

    @Transactional
    public void startBonusPoints(String roomName, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.TRICK_RESULTS)){
            throw new BadActionException("Bonus points can only be entered from the trick results phase");
        }

        int submittedResults = teamSessionEventRepository.countBySessionEvent(currentEvent);
        if(submittedResults < session.getTeams().size()){
            throw new BadActionException("All teams must submit their tricks won before continuing");
        }

        SessionEventPayload.TrickResults trickResultsPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.TrickResults.class);
        int cardCount = trickResultsPhase.cardCount();
        int totalTricks = sumTricksWon(currentEvent);
        if(totalTricks != cardCount){
            throw new BadActionException("Total tricks won (" + totalTricks + ") must equal the " + cardCount + " cards dealt this round");
        }

        sessionEventService.createSessionEvent(
            session,
            SessionEventType.BONUS_POINTS,
            new SessionEventPayload.BonusPoints(trickResultsPhase.round(), cardCount));

        publishStateChanged(room, session);
    }

    @Transactional
    public void submitBonusPoints(String roomName, SubmitBonusPointsRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);
        Team team = teamUtilityService.getOrThrowTeamById(request.getTeamId());

        throwIfUserCantSubmitForTeam(room, roomUser, team);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.BONUS_POINTS)){
            throw new BadActionException("Bonus points can only be submitted during the bonus points phase");
        }

        validateBonusRanges(request);

        // Bonuses require making a positive bid: a missed bid earns nothing, and a
        // zero bid means zero tricks taken, so no cards were captured to earn bonuses.
        if(!eligibleForBonus(session, team) && hasAnyBonus(request)){
            throw new BadActionException("This team can't earn bonus points — they must make a bid of one or more");
        }

        teamSessionEventService.upsertTeamSessionEvent(
            session,
            currentEvent,
            team,
            TeamSessionEventType.BONUS_POINTS,
            new TeamSessionEventPayload.BonusPoints(
                request.getStandardFourteens(),
                request.getBlackFourteen(),
                request.getMermaidsByPirate(),
                request.getPiratesBySkullKing(),
                request.getSkullKingByMermaid(),
                request.getLoot()));

        publishStateChanged(room, session);
    }

    @Transactional
    public void finishRound(String roomName, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent currentEvent = sessionEventService.getOrThrowCurrentEvent(session);
        if(!currentEvent.getType().equals(SessionEventType.BONUS_POINTS)){
            throw new BadActionException("A round can only be finished from the bonus points phase");
        }

        int submittedBonuses = teamSessionEventRepository.countBySessionEvent(currentEvent);
        if(submittedBonuses < session.getTeams().size()){
            throw new BadActionException("All teams must have their bonus points recorded before finishing the round");
        }

        validateLootTotals(currentEvent, session.getTeams().size());

        SessionEventPayload.BonusPoints bonusPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.BonusPoints.class);

        scoreRound(session, currentEvent, bonusPhase.cardCount());

        if(bonusPhase.round() >= MAX_ROUNDS){
            completeSession(session, room);
            return;
        }

        int nextRound = bonusPhase.round() + 1;
        sessionEventService.createSessionEvent(
            session,
            SessionEventType.BIDS,
            new SessionEventPayload.Bids(nextRound, nextRound));

        publishStateChanged(room, session);
    }

    private void completeSession(Session session, Room room){
        sessionUtilitysService.completeSession(session);
        roomsUtilityService.changeRoomStatus(room, RoomStatus.COMPLETED);
        // The session is no longer IN_PROGRESS, so we broadcast a room update
        // (clients switch to the final scoreboard) rather than a game-state update.
        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    private void scoreRound(Session session, SessionEvent bonusEvent, int cardCount){
        SessionEvent bidsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS).orElseThrow();
        SessionEvent trickResultsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS).orElseThrow();

        for(Team team : session.getTeams()){
            Integer bid = teamValue(bidsEvent, team, TeamSessionEventType.BIDS);
            Integer tricksWon = teamValue(trickResultsEvent, team, TeamSessionEventType.TRICK_RESULTS);
            if(bid == null || tricksWon == null) continue;

            long roundScore = scoreTeamRound(bid, tricksWon, cardCount, teamBonus(bonusEvent, team));
            teamUtilityService.addToScore(team, roundScore);
        }
    }

    private long scoreTeamRound(int bid, int tricksWon, int cardCount, TeamSessionEventPayload.BonusPoints bonus){
        boolean madeBid = bid == tricksWon;

        if(bid == 0){
            // A zero bid scores ±10 per card dealt; no bonuses are possible with zero tricks.
            return madeBid ? 10L * cardCount : -10L * cardCount;
        }

        if(!madeBid){
            return -10L * Math.abs(bid - tricksWon);
        }

        // Made a positive bid: 20 per trick plus any bonuses earned.
        return 20L * bid + bonusValue(bonus);
    }

    private long bonusValue(TeamSessionEventPayload.BonusPoints bonus){
        if(bonus == null) return 0;
        return bonus.standardFourteens() * 10L
            + (bonus.blackFourteen() ? 20L : 0L)
            + bonus.mermaidsByPirate() * 20L
            + bonus.piratesBySkullKing() * 30L
            + (bonus.skullKingByMermaid() ? 40L : 0L)
            + bonus.loot() * 20L;
    }

    private TeamSessionEventPayload.BonusPoints teamBonus(SessionEvent bonusEvent, Team team){
        return teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent).stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BONUS_POINTS))
            .filter(event -> event.getTeam().getId().equals(team.getId()))
            .map(event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.BonusPoints.class))
            .findFirst()
            .orElse(null);
    }

    private void validateBonusRanges(SubmitBonusPointsRequest request){
        if(request.getStandardFourteens() < 0 || request.getStandardFourteens() > 3){
            throw new BadActionException("Standard 14s captured must be between 0 and 3");
        }
        if(request.getMermaidsByPirate() < 0 || request.getMermaidsByPirate() > 2){
            throw new BadActionException("Mermaids captured by a pirate must be between 0 and 2");
        }
        if(request.getPiratesBySkullKing() < 0 || request.getPiratesBySkullKing() > 5){
            throw new BadActionException("Pirates captured by the Skull King must be between 0 and 5");
        }
        if(request.getLoot() < 0 || request.getLoot() > 2){
            throw new BadActionException("Loot per team must be between 0 and 2");
        }
    }

    private boolean hasAnyBonus(SubmitBonusPointsRequest request){
        return request.getStandardFourteens() > 0
            || request.getBlackFourteen()
            || request.getMermaidsByPirate() > 0
            || request.getPiratesBySkullKing() > 0
            || request.getSkullKingByMermaid()
            || request.getLoot() > 0;
    }

    private void validateLootTotals(SessionEvent bonusEvent, int teamCount){
        List<Integer> loots = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent).stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BONUS_POINTS))
            .map(event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.BonusPoints.class).loot())
            .toList();

        int total = loots.stream().mapToInt(Integer::intValue).sum();
        int max = loots.stream().mapToInt(Integer::intValue).max().orElse(0);

        // Every loot alliance pays exactly two teams, so credits come in pairs:
        // total must be even, at most 4 (two loot cards), and pairable (no team
        // can hold more credits than there are alliances).
        if(total % 2 != 0){
            throw new BadActionException("Loot coins must total an even number — every alliance pays two teams");
        }
        if(total > 4){
            throw new BadActionException("There are only two Loot cards, so at most 4 loot coins can be awarded");
        }
        if(total > 0 && max > total / 2){
            throw new BadActionException("Loot coins can't be paired — a team holds more coins than there are alliances");
        }
    }

    private boolean eligibleForBonus(Session session, Team team){
        SessionEvent bidsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.BIDS).orElse(null);
        SessionEvent trickResultsEvent = sessionEventService.findLatestEventOfType(session, SessionEventType.TRICK_RESULTS).orElse(null);
        if(bidsEvent == null || trickResultsEvent == null) return false;

        Integer bid = teamValue(bidsEvent, team, TeamSessionEventType.BIDS);
        Integer tricksWon = teamValue(trickResultsEvent, team, TeamSessionEventType.TRICK_RESULTS);
        if(bid == null || tricksWon == null) return false;

        // A zero bid takes no tricks, so no cards are captured — no bonuses possible.
        return bid > 0 && bid.equals(tricksWon);
    }

    private Integer teamValue(SessionEvent sessionEvent, Team team, TeamSessionEventType type){
        return teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(sessionEvent).stream()
            .filter(event -> event.getType().equals(type))
            .filter(event -> event.getTeam().getId().equals(team.getId()))
            .map(event -> type.equals(TeamSessionEventType.BIDS)
                ? objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.Bids.class).bid()
                : objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.TrickResults.class).tricksWon())
            .findFirst()
            .orElse(null);
    }

    private int sumTricksWon(SessionEvent trickResultsEvent){
        return teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(trickResultsEvent).stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.TRICK_RESULTS))
            .mapToInt(event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.TrickResults.class).tricksWon())
            .sum();
    }

    private int teamTricksWon(SessionEvent trickResultsEvent, Team team){
        Integer value = teamValue(trickResultsEvent, team, TeamSessionEventType.TRICK_RESULTS);
        return value == null ? 0 : value;
    }

    private void publishStateChanged(Room room, Session session){
        eventPublisher.publishEvent(new SkullKingStateChangedEvent(room.getName(), session.getId()));
    }

    private void throwIfUserCantSubmitForTeam(Room room, RoomUser roomUser, Team team){
        // The admin is the room authority and may submit for any team (they act as
        // the fallback when advancing a phase, regardless of tracking mode). Other
        // players may only submit for their own team.
        if(roomUser.getRole().equals(RoomUserRoles.ADMIN)){
            return;
        }

        if(roomUser.getTeam() == null || !roomUser.getTeam().getId().equals(team.getId())){
            throw new UnauthorizedException("You can only submit events for your own team");
        }
    }
}
