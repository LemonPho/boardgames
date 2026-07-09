package com.motomutterers.boardgames.sessions.services;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.UnauthorizedException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.teamsessionevent.CreateTeamSessionEventRequest;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.teams.models.Team;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.transaction.Transactional;

@Service
public class TeamSessionEventService {
    private final SessionUtilitysService sessionUtilitysService;
    private final RoomsUtilityService roomsUtilityService;
    private final TeamUtilityService teamUtilityService;
    private final UserService userService;


    public TeamSessionEventService(
        SessionUtilitysService sessionUtilitysService,
        RoomsUtilityService roomsUtilityService,
        TeamUtilityService teamUtilityService,
        UserService userService
    ){
        this.sessionUtilitysService = sessionUtilitysService;
        this.roomsUtilityService = roomsUtilityService;
        this.teamUtilityService = teamUtilityService;
        this.userService = userService;
    }

    @Transactional
    public void createTeamSessionEvent(
        String sessionId,
        CreateTeamSessionEventRequest request,
        Authentication authentication
    ){
        User user = userService.getAuthenticatedUser(authentication);
        Session session = sessionUtilitysService.getOrThrowSessionById(sessionId);
        Room room = session.getRoom();
        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        Team team = teamUtilityService.getOrThrowTeamById(sessionId);

        if(!roomUser.equals(team.getRoomUser())){
            throw new UnauthorizedException("You do not have permission to modify team events on this team");
        }
    }
}
