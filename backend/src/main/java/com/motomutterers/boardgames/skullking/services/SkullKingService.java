package com.motomutterers.boardgames.skullking.services;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
import com.motomutterers.boardgames.skullking.dto.CorrectBidsRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectBonusRequest;
import com.motomutterers.boardgames.skullking.dto.CorrectTricksRequest;
import com.motomutterers.boardgames.skullking.dto.RoundHistoryResponse;
import com.motomutterers.boardgames.skullking.dto.RoundHistoryTeamResponse;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.dto.SubmitBidRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitBonusPointsRequest;
import com.motomutterers.boardgames.skullking.dto.SubmitTrickResultRequest;
import com.motomutterers.boardgames.skullking.dto.TeamBonusResponse;
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
    // Base deck: 56 suited (4×14) + 14 special. Advanced play adds Loot ×2, Kraken,
    // and White Whale (+4). Deck size caps how many cards each player can be dealt.
    private static final int BASE_DECK_SIZE = 70;
    private static final int ADVANCED_DECK_SIZE = 74;

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
        // Randomly choose which seat (in join order) leads the first round.
        List<Team> ordered = orderedTeams(session);
        String startingTeamId = ordered.isEmpty()
            ? null
            : ordered.get(ThreadLocalRandom.current().nextInt(ordered.size())).getId().toString();

        sessionEventService.createSessionEvent(session, SessionEventType.BIDS, new SessionEventPayload.Bids(1, cardCountForRound(session, 1), startingTeamId));
    }

    // Cards dealt per player in a round. Normally the round number, but the deck is
    // finite, so a full table can't be dealt `round` cards each in the late rounds —
    // cap at how many the deck allows everyone to receive equally. The advanced-cards
    // room setting enlarges the deck.
    private int cardCountForRound(Session session, int round){
        int players = Math.max(1, session.getTeams().size());
        boolean advanced = session.getRoom().getConfiguration() != null
            && session.getRoom().getConfiguration().getAdvancedCards();
        int deckSize = advanced ? ADVANCED_DECK_SIZE : BASE_DECK_SIZE;
        return Math.min(round, deckSize / players);
    }

    // Teams in a stable display/turn order: by their player's join time ascending.
    private List<Team> orderedTeams(Session session){
        return session.getTeams().stream()
            .sorted(Comparator.comparing(
                t -> t.getRoomUser() != null ? t.getRoomUser().getJoinedAt() : null,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    // The leader of the next round is the next seat in join order after this round's leader.
    private String nextRoundStartingTeamId(Session session, String currentStartingTeamId){
        List<Team> ordered = orderedTeams(session);
        if(ordered.isEmpty()) return null;

        int currentIndex = 0;
        for(int i = 0; i < ordered.size(); i++){
            if(ordered.get(i).getId().toString().equals(currentStartingTeamId)){
                currentIndex = i;
                break;
            }
        }
        return ordered.get((currentIndex + 1) % ordered.size()).getId().toString();
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

    public RoundHistoryResponse getRoundHistory(String roomName, int round, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        // Each round is a run of session events (BIDS → IN_PROGRESS → TRICK_RESULTS →
        // BONUS_POINTS), each payload carrying its round number. Corrections overwrite
        // team events in place, so there's exactly one session event of each type per round.
        List<SessionEvent> events = sessionEventService.findAllEvents(session);

        SessionEvent bidsEvent = findRoundEvent(events, round, SessionEventType.BIDS);
        if(bidsEvent == null){
            throw new BadActionException("Round " + round + " has not started yet");
        }
        SessionEvent trickResultsEvent = findRoundEvent(events, round, SessionEventType.TRICK_RESULTS);
        SessionEvent bonusEvent = findRoundEvent(events, round, SessionEventType.BONUS_POINTS);

        SessionEventPayload.Bids bidsPayload = objectMapper.readValue(bidsEvent.getPayload(), SessionEventPayload.Bids.class);
        boolean completed = bonusEvent != null;
        boolean krakenPlayed = trickResultsEvent != null
            && objectMapper.readValue(trickResultsEvent.getPayload(), SessionEventPayload.TrickResults.class).krakenPlayed();

        List<RoundHistoryTeamResponse> teamRows = orderedTeams(session).stream()
            .map(team -> buildRoundTeamRow(team, bidsPayload.cardCount(), bidsEvent, trickResultsEvent, bonusEvent))
            .toList();

        return new RoundHistoryResponse(bidsPayload.round(), bidsPayload.cardCount(), completed, krakenPlayed, bidsPayload.startingTeamId(), teamRows);
    }

    // --- Past-round corrections (admin only; each section validated and saved as a set) ---

    @Transactional
    public void correctBids(String roomName, int round, CorrectBidsRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent bidsEvent = requireRoundEvent(session, round, SessionEventType.BIDS);
        int cardCount = roundCardCount(bidsEvent);

        for(CorrectBidsRequest.TeamValue tv : request.getTeams()){
            if(tv.getValue() < 0 || tv.getValue() > cardCount){
                throw new BadActionException("Bid must be between 0 and " + cardCount);
            }
        }

        for(CorrectBidsRequest.TeamValue tv : request.getTeams()){
            Team team = teamUtilityService.getOrThrowTeamById(tv.getTeamId());
            teamSessionEventService.upsertTeamSessionEvent(
                session, bidsEvent, team, TeamSessionEventType.BIDS,
                new TeamSessionEventPayload.Bids(tv.getValue()));
        }

        // A team that becomes ineligible keeps its stored bonus untouched; scoring
        // already ignores bonuses unless the team makes a positive bid, so the bonus
        // simply reappears if the team qualifies again.
        recomputeScores(session);
        publishStateChanged(room, session);
    }

    @Transactional
    public void correctTricks(String roomName, int round, CorrectTricksRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent bidsEvent = requireRoundEvent(session, round, SessionEventType.BIDS);
        SessionEvent tricksEvent = requireRoundEvent(session, round, SessionEventType.TRICK_RESULTS);
        int cardCount = roundCardCount(bidsEvent);
        SessionEventPayload.TrickResults phase = objectMapper.readValue(tricksEvent.getPayload(), SessionEventPayload.TrickResults.class);
        int expected = expectedTrickTotal(phase);

        int total = 0;
        for(CorrectTricksRequest.TeamValue tv : request.getTeams()){
            if(tv.getValue() < 0 || tv.getValue() > cardCount){
                throw new BadActionException("Tricks won must be between 0 and " + cardCount);
            }
            total += tv.getValue();
        }
        if(total != expected){
            throw new BadActionException("Total tricks won (" + total + ") must equal " + expected + trickTotalReason(phase));
        }

        for(CorrectTricksRequest.TeamValue tv : request.getTeams()){
            Team team = teamUtilityService.getOrThrowTeamById(tv.getTeamId());
            teamSessionEventService.upsertTeamSessionEvent(
                session, tricksEvent, team, TeamSessionEventType.TRICK_RESULTS,
                new TeamSessionEventPayload.TrickResults(tv.getValue()));
        }

        // Ineligible teams keep their stored bonus (see correctBids) — scoring ignores
        // it while ineligible and restores it if the team qualifies again.
        recomputeScores(session);
        publishStateChanged(room, session);
    }

    @Transactional
    public void setKraken(String roomName, int round, boolean krakenPlayed, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent tricksEvent = requireRoundEvent(session, round, SessionEventType.TRICK_RESULTS);
        SessionEventPayload.TrickResults phase = objectMapper.readValue(tricksEvent.getPayload(), SessionEventPayload.TrickResults.class);

        sessionEventService.updatePayload(
            tricksEvent,
            new SessionEventPayload.TrickResults(phase.round(), phase.cardCount(), phase.startingTeamId(), krakenPlayed));

        publishStateChanged(room, session);
    }

    @Transactional
    public void correctBonus(String roomName, int round, CorrectBonusRequest request, Authentication authentication){
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);

        SessionEvent bidsEvent = requireRoundEvent(session, round, SessionEventType.BIDS);
        SessionEvent tricksEvent = requireRoundEvent(session, round, SessionEventType.TRICK_RESULTS);
        SessionEvent bonusEvent = requireRoundEvent(session, round, SessionEventType.BONUS_POINTS);

        for(CorrectBonusRequest.TeamBonusValue tv : request.getTeams()){
            validateBonusRange(tv);
            boolean hasBonus = bonusHasAny(tv);
            if(hasBonus && !roundEligibleForBonus(bidsEvent, tricksEvent, tv.getTeamId())){
                throw new BadActionException("A team can only earn bonus points if it made a bid of one or more");
            }
        }

        int lootTotal = request.getTeams().stream().mapToInt(CorrectBonusRequest.TeamBonusValue::getLoot).sum();
        int lootMax = request.getTeams().stream().mapToInt(CorrectBonusRequest.TeamBonusValue::getLoot).max().orElse(0);
        validateLoot(lootTotal, lootMax);

        validateBonusCardLimits(
            request.getTeams().stream().mapToInt(CorrectBonusRequest.TeamBonusValue::getStandardFourteens).sum(),
            (int) request.getTeams().stream().filter(CorrectBonusRequest.TeamBonusValue::getBlackFourteen).count(),
            request.getTeams().stream().mapToInt(CorrectBonusRequest.TeamBonusValue::getMermaidsByPirate).sum(),
            request.getTeams().stream().mapToInt(CorrectBonusRequest.TeamBonusValue::getPiratesBySkullKing).sum(),
            (int) request.getTeams().stream().filter(CorrectBonusRequest.TeamBonusValue::getSkullKingByMermaid).count());

        for(CorrectBonusRequest.TeamBonusValue tv : request.getTeams()){
            Team team = teamUtilityService.getOrThrowTeamById(tv.getTeamId());
            teamSessionEventService.upsertTeamSessionEvent(
                session, bonusEvent, team, TeamSessionEventType.BONUS_POINTS,
                new TeamSessionEventPayload.BonusPoints(
                    tv.getStandardFourteens(), tv.getBlackFourteen(), tv.getMermaidsByPirate(),
                    tv.getPiratesBySkullKing(), tv.getSkullKingByMermaid(), tv.getLoot()));
        }

        recomputeScores(session);
        publishStateChanged(room, session);
    }

    private SessionEvent requireRoundEvent(Session session, int round, SessionEventType type){
        SessionEvent event = findRoundEvent(sessionEventService.findAllEvents(session), round, type);
        if(event == null){
            throw new BadActionException("Round " + round + " does not have a " + type + " phase to correct");
        }
        return event;
    }

    private int roundCardCount(SessionEvent bidsEvent){
        return objectMapper.readValue(bidsEvent.getPayload(), SessionEventPayload.Bids.class).cardCount();
    }

    private boolean roundEligibleForBonus(SessionEvent bidsEvent, SessionEvent tricksEvent, String teamId){
        Integer bid = teamValueById(bidsEvent, teamId, TeamSessionEventType.BIDS);
        Integer tricksWon = teamValueById(tricksEvent, teamId, TeamSessionEventType.TRICK_RESULTS);
        if(bid == null || tricksWon == null) return false;
        return bid > 0 && bid.equals(tricksWon);
    }

    // Recomputes every team's finalScore from scratch by replaying all fully-entered
    // rounds. This is the single source of truth for scores — used after finishing a
    // round and after any correction — so edits can never drift from the accumulator.
    private void recomputeScores(Session session){
        List<SessionEvent> events = sessionEventService.findAllEvents(session);

        List<Integer> rounds = events.stream()
            .filter(e -> e.getType().equals(SessionEventType.BIDS))
            .map(this::eventRound)
            .distinct()
            .toList();

        for(Team team : session.getTeams()){
            long total = 0;
            for(int round : rounds){
                SessionEvent bidsEvent = findRoundEvent(events, round, SessionEventType.BIDS);
                SessionEvent tricksEvent = findRoundEvent(events, round, SessionEventType.TRICK_RESULTS);
                SessionEvent bonusEvent = findRoundEvent(events, round, SessionEventType.BONUS_POINTS);
                if(bidsEvent == null || tricksEvent == null) continue; // round not scorable yet

                Integer bid = teamValue(bidsEvent, team, TeamSessionEventType.BIDS);
                Integer tricksWon = teamValue(tricksEvent, team, TeamSessionEventType.TRICK_RESULTS);
                if(bid == null || tricksWon == null) continue;

                TeamSessionEventPayload.BonusPoints bonus = bonusEvent == null ? null : teamBonus(bonusEvent, team);
                total += scoreTeamRound(bid, tricksWon, roundCardCount(bidsEvent), bonus);
            }
            teamUtilityService.setScore(team, total);
        }
    }

    private RoundHistoryTeamResponse buildRoundTeamRow(
        Team team,
        int cardCount,
        SessionEvent bidsEvent,
        SessionEvent trickResultsEvent,
        SessionEvent bonusEvent
    ){
        Integer bid = teamValue(bidsEvent, team, TeamSessionEventType.BIDS);
        Integer tricksWon = trickResultsEvent == null ? null : teamValue(trickResultsEvent, team, TeamSessionEventType.TRICK_RESULTS);
        TeamSessionEventPayload.BonusPoints bonus = bonusEvent == null ? null : teamBonus(bonusEvent, team);

        // The round only contributes a score once it's fully entered (bids + tricks).
        long roundScore = (bid != null && tricksWon != null)
            ? scoreTeamRound(bid, tricksWon, cardCount, bonus)
            : 0;

        TeamBonusResponse bonusResponse = bonus == null ? null : new TeamBonusResponse(
            bonus.standardFourteens(),
            bonus.blackFourteen(),
            bonus.mermaidsByPirate(),
            bonus.piratesBySkullKing(),
            bonus.skullKingByMermaid(),
            bonus.loot()
        );

        return new RoundHistoryTeamResponse(
            team.getId(),
            team.getRoomUser() != null ? team.getRoomUser().getDisplayName() : null,
            bid,
            tricksWon,
            bonusResponse,
            roundScore
        );
    }

    private SessionEvent findRoundEvent(List<SessionEvent> events, int round, SessionEventType type){
        return events.stream()
            .filter(event -> event.getType().equals(type))
            .filter(event -> eventRound(event) == round)
            .reduce((first, second) -> second) // latest wins if somehow duplicated
            .orElse(null);
    }

    private int eventRound(SessionEvent event){
        return switch(event.getType()){
            case BIDS -> objectMapper.readValue(event.getPayload(), SessionEventPayload.Bids.class).round();
            case IN_PROGRESS -> objectMapper.readValue(event.getPayload(), SessionEventPayload.InProgress.class).round();
            case TRICK_RESULTS -> objectMapper.readValue(event.getPayload(), SessionEventPayload.TrickResults.class).round();
            case BONUS_POINTS -> objectMapper.readValue(event.getPayload(), SessionEventPayload.BonusPoints.class).round();
        };
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
            new SessionEventPayload.InProgress(bidsPhase.round(), bidsPhase.cardCount(), bidsPhase.startingTeamId()));

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
            new SessionEventPayload.TrickResults(inProgressPhase.round(), inProgressPhase.cardCount(), inProgressPhase.startingTeamId(), false));

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
        int expected = expectedTrickTotal(trickResultsPhase);
        int totalTricks = sumTricksWon(currentEvent);
        if(totalTricks != expected){
            throw new BadActionException("Total tricks won (" + totalTricks + ") must equal " + expected + trickTotalReason(trickResultsPhase));
        }

        sessionEventService.createSessionEvent(
            session,
            SessionEventType.BONUS_POINTS,
            new SessionEventPayload.BonusPoints(trickResultsPhase.round(), trickResultsPhase.cardCount(), trickResultsPhase.startingTeamId()));

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
        validateBonusTotals(currentEvent);

        SessionEventPayload.BonusPoints bonusPhase = objectMapper.readValue(currentEvent.getPayload(), SessionEventPayload.BonusPoints.class);

        recomputeScores(session);

        if(bonusPhase.round() >= MAX_ROUNDS){
            completeSession(session, room);
            return;
        }

        int nextRound = bonusPhase.round() + 1;
        String nextStartingTeamId = nextRoundStartingTeamId(session, bonusPhase.startingTeamId());
        sessionEventService.createSessionEvent(
            session,
            SessionEventType.BIDS,
            new SessionEventPayload.Bids(nextRound, cardCountForRound(session, nextRound), nextStartingTeamId));

        publishStateChanged(room, session);
    }

    private void completeSession(Session session, Room room){
        sessionUtilitysService.completeSession(session);
        roomsUtilityService.changeRoomStatus(room, RoomStatus.COMPLETED);
        // The session is no longer IN_PROGRESS, so we broadcast a room update
        // (clients switch to the final scoreboard) rather than a game-state update.
        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
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

    // Each bonus source is a fixed set of unique cards in the deck, so the totals
    // captured across ALL teams in a single round can't exceed what exists: three
    // coloured 14s, one black 14, two mermaids, five pirates, one Skull King. The
    // per-team ranges guard each entry; this guards the round-wide sum.
    private void validateBonusCardLimits(int standardFourteens, int blackFourteens,
            int mermaidsByPirate, int piratesBySkullKing, int skullKingsByMermaid){
        if(standardFourteens > 3){
            throw new BadActionException("Only three standard 14s exist, so at most three can be captured in a round");
        }
        if(blackFourteens > 1){
            throw new BadActionException("There is only one black 14, so only one team can capture it");
        }
        if(mermaidsByPirate > 2){
            throw new BadActionException("There are only two mermaids to be captured by a pirate");
        }
        if(piratesBySkullKing > 5){
            throw new BadActionException("There are only five pirates to be captured by the Skull King");
        }
        if(skullKingsByMermaid > 1){
            throw new BadActionException("There is only one Skull King, so only one team can capture it with a mermaid");
        }
    }

    private void validateBonusTotals(SessionEvent bonusEvent){
        List<TeamSessionEventPayload.BonusPoints> bonuses = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent).stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BONUS_POINTS))
            .map(event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.BonusPoints.class))
            .toList();

        validateBonusCardLimits(
            bonuses.stream().mapToInt(TeamSessionEventPayload.BonusPoints::standardFourteens).sum(),
            (int) bonuses.stream().filter(TeamSessionEventPayload.BonusPoints::blackFourteen).count(),
            bonuses.stream().mapToInt(TeamSessionEventPayload.BonusPoints::mermaidsByPirate).sum(),
            bonuses.stream().mapToInt(TeamSessionEventPayload.BonusPoints::piratesBySkullKing).sum(),
            (int) bonuses.stream().filter(TeamSessionEventPayload.BonusPoints::skullKingByMermaid).count());
    }

    private void validateLootTotals(SessionEvent bonusEvent, int teamCount){
        List<Integer> loots = teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(bonusEvent).stream()
            .filter(event -> event.getType().equals(TeamSessionEventType.BONUS_POINTS))
            .map(event -> objectMapper.readValue(event.getPayload(), TeamSessionEventPayload.BonusPoints.class).loot())
            .toList();

        validateLoot(loots.stream().mapToInt(Integer::intValue).sum(),
                     loots.stream().mapToInt(Integer::intValue).max().orElse(0));
    }

    // Every loot alliance pays exactly two teams, so credits come in pairs: the
    // total must be even, at most 4 (two loot cards), and pairable (no team can
    // hold more credits than there are alliances).
    private void validateLoot(int total, int max){
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

    private void validateBonusRange(CorrectBonusRequest.TeamBonusValue tv){
        if(tv.getStandardFourteens() < 0 || tv.getStandardFourteens() > 3){
            throw new BadActionException("Standard 14s captured must be between 0 and 3");
        }
        if(tv.getMermaidsByPirate() < 0 || tv.getMermaidsByPirate() > 2){
            throw new BadActionException("Mermaids captured by a pirate must be between 0 and 2");
        }
        if(tv.getPiratesBySkullKing() < 0 || tv.getPiratesBySkullKing() > 5){
            throw new BadActionException("Pirates captured by the Skull King must be between 0 and 5");
        }
        if(tv.getLoot() < 0 || tv.getLoot() > 2){
            throw new BadActionException("Loot per team must be between 0 and 2");
        }
    }

    private boolean bonusHasAny(CorrectBonusRequest.TeamBonusValue tv){
        return tv.getStandardFourteens() > 0 || tv.getBlackFourteen() || tv.getMermaidsByPirate() > 0
            || tv.getPiratesBySkullKing() > 0 || tv.getSkullKingByMermaid() || tv.getLoot() > 0;
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
        return teamValueById(sessionEvent, team.getId().toString(), type);
    }

    private Integer teamValueById(SessionEvent sessionEvent, String teamId, TeamSessionEventType type){
        return teamSessionEventRepository.findBySessionEventOrderBySequenceAsc(sessionEvent).stream()
            .filter(event -> event.getType().equals(type))
            .filter(event -> event.getTeam().getId().toString().equals(teamId))
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

    // A Kraken destroys one trick (won by nobody), so the teams' tricks must sum to
    // one fewer than the cards dealt.
    private int expectedTrickTotal(SessionEventPayload.TrickResults phase){
        return phase.cardCount() - (phase.krakenPlayed() ? 1 : 0);
    }

    private String trickTotalReason(SessionEventPayload.TrickResults phase){
        return phase.krakenPlayed()
            ? " (" + phase.cardCount() + " cards − 1 Kraken trick)"
            : " — the " + phase.cardCount() + " cards dealt this round";
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
