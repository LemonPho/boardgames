package com.motomutterers.boardgames.sessions.services;

import org.springframework.stereotype.Service;

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

    /**
     * Records a team's response for the current phase. If the team has already
     * responded, the existing row is overwritten in place (a correction); this
     * keeps one row per team per phase (see the unique constraint) while letting
     * a value be corrected in the moment.
     */
    @Transactional
    public TeamSessionEvent upsertTeamSessionEvent(
        Session session,
        SessionEvent sessionEvent,
        Team team,
        TeamSessionEventType type,
        TeamSessionEventPayload payload
    ){
        String payloadJson = objectMapper.writeValueAsString(payload);

        TeamSessionEvent teamSessionEvent = teamSessionEventRepository
            .findBySessionEventAndTeam(sessionEvent, team)
            .orElse(null);

        if(teamSessionEvent == null){
            int nextSequence = teamSessionEventRepository.countBySession(session) + 1;
            teamSessionEvent = new TeamSessionEvent(session, sessionEvent, team, type, nextSequence, payloadJson);
        } else {
            teamSessionEvent.setPayload(payloadJson);
        }

        teamSessionEventRepository.save(teamSessionEvent);
        roomsUtilityService.updateRoomLastUpdated(session.getRoom());

        return teamSessionEvent;
    }
}
