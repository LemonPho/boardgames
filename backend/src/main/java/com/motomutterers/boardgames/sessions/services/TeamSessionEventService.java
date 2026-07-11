package com.motomutterers.boardgames.sessions.services;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.teams.models.Team;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class TeamSessionEventService {
    private final TeamSessionEventRepository teamSessionEventRepository;
    private final RoomsUtilityService roomsUtilityService;
    private final ObjectMapper objectMapper;

    public TeamSessionEventService(
        TeamSessionEventRepository teamSessionEventRepository,
        RoomsUtilityService roomsUtilityService,
        ObjectMapper objectMapper
    ){
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TeamSessionEvent createTeamSessionEvent(
        Session session,
        SessionEvent sessionEvent,
        Team team,
        TeamSessionEventType type,
        TeamSessionEventPayload payload
    ){
        int nextSequence = teamSessionEventRepository.countBySession(session) + 1;

        TeamSessionEvent teamSessionEvent = new TeamSessionEvent(
            session,
            sessionEvent,
            team,
            type,
            nextSequence,
            objectMapper.writeValueAsString(payload)
        );

        teamSessionEventRepository.save(teamSessionEvent);
        roomsUtilityService.updateRoomLastUpdated(session.getRoom());

        return teamSessionEvent;
    }

    public void throwIfTeamAlreadyResponded(SessionEvent sessionEvent, Team team){
        if(teamSessionEventRepository.existsBySessionEventAndTeam(sessionEvent, team)){
            throw new BadActionException("This team has already responded to the current event");
        }
    }
}
