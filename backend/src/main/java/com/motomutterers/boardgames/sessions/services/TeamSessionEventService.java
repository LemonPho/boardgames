package com.motomutterers.boardgames.sessions.services;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.VersionConflictException;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEvent;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventPayload;
import com.motomutterers.boardgames.sessions.models.teamsessionevent.TeamSessionEventType;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.sessions.repositories.TeamSessionEventRepository;
import com.motomutterers.boardgames.teams.models.Team;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class TeamSessionEventService {
    /**
     * How a team-value write treats the row's version.
     */
    public enum WriteMode {
        // Server default at a phase change: only inserts a missing row, at version 0, and
        // never bumps. A late grace-window save made against "no value yet" (version 0)
        // must still match afterwards.
        PREFILL,
        // Admin batch correction: always applies and bumps.
        AUTHORITY,
        // Live submit (admin or player): applies only if the expected version matches the
        // row's current one (null skips the check), then bumps.
        CHECKED
    }

    private final TeamSessionEventRepository teamSessionEventRepository;
    private final SessionRepository sessionRepository;
    private final RoomsUtilityService roomsUtilityService;
    private final ObjectMapper objectMapper;

    public TeamSessionEventService(
        TeamSessionEventRepository teamSessionEventRepository,
        SessionRepository sessionRepository,
        RoomsUtilityService roomsUtilityService,
        ObjectMapper objectMapper
    ){
        this.teamSessionEventRepository = teamSessionEventRepository;
        this.sessionRepository = sessionRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.objectMapper = objectMapper;
    }

    /**
     * Records a team's value for a phase, keeping one row per team per phase (see the unique
     * constraint): an existing row is overwritten in place. Returns the row's version after
     * the write.
     *
     * @throws VersionConflictException for a CHECKED write whose non-null expectedVersion
     *     isn't the row's current version (0 when there's no row). Nothing is written.
     */
    @Transactional
    public int writeTeamSessionEvent(
        Session session,
        SessionEvent sessionEvent,
        Team team,
        TeamSessionEventType type,
        TeamSessionEventPayload payload,
        WriteMode mode,
        Integer expectedVersion
    ){
        // Lock the session row before reading anything, so writes to one session run one at a
        // time. Two concurrent saves for a team can then never both pass the version check.
        // This also fixes the sequence-number collision: two first writes of a phase at the
        // same moment used to both read the same countBySession + 1, and the second insert
        // violated uq_team_session_events_sequence, giving an unhandled 500.
        sessionRepository.lockById(session.getId());

        TeamSessionEvent teamSessionEvent = teamSessionEventRepository
            .findBySessionEventAndTeam(sessionEvent, team)
            .orElse(null);

        if(mode == WriteMode.PREFILL){
            // A real entry (or an earlier fill-in) is never overwritten or bumped.
            if(teamSessionEvent != null) return teamSessionEvent.getVersion();
            return insert(session, sessionEvent, team, type, payload, 0).getVersion();
        }

        if(mode == WriteMode.CHECKED){
            int currentVersion = teamSessionEvent == null ? 0 : teamSessionEvent.getVersion();
            if(expectedVersion != null && expectedVersion != currentVersion){
                throw new VersionConflictException(
                    team.getId(), type, currentVersion,
                    teamSessionEvent == null ? null : readPayload(teamSessionEvent.getPayload(), type));
            }
        }

        if(teamSessionEvent == null){
            return insert(session, sessionEvent, team, type, payload, 1).getVersion();
        }

        teamSessionEvent.setPayload(objectMapper.writeValueAsString(payload));
        teamSessionEvent.setVersion(teamSessionEvent.getVersion() + 1);
        teamSessionEventRepository.save(teamSessionEvent);
        roomsUtilityService.updateRoomLastUpdated(session.getRoom());
        return teamSessionEvent.getVersion();
    }

    private TeamSessionEvent insert(
        Session session,
        SessionEvent sessionEvent,
        Team team,
        TeamSessionEventType type,
        TeamSessionEventPayload payload,
        int version
    ){
        // Safe only under the session lock taken by the caller (see writeTeamSessionEvent).
        int nextSequence = teamSessionEventRepository.countBySession(session) + 1;
        TeamSessionEvent teamSessionEvent = new TeamSessionEvent(
            session, sessionEvent, team, type, nextSequence, objectMapper.writeValueAsString(payload));
        teamSessionEvent.setVersion(version);
        teamSessionEventRepository.save(teamSessionEvent);
        roomsUtilityService.updateRoomLastUpdated(session.getRoom());
        return teamSessionEvent;
    }

    private TeamSessionEventPayload readPayload(String json, TeamSessionEventType type){
        return switch(type){
            case BIDS -> objectMapper.readValue(json, TeamSessionEventPayload.Bids.class);
            case TRICK_RESULTS -> objectMapper.readValue(json, TeamSessionEventPayload.TrickResults.class);
            case BONUS_POINTS -> objectMapper.readValue(json, TeamSessionEventPayload.BonusPoints.class);
        };
    }
}
