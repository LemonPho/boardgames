package com.motomutterers.boardgames.sessions.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.dto.CreateSessionRequest;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.events.SessionUpdatedEvent;
import com.motomutterers.boardgames.sessions.models.Session;
import com.motomutterers.boardgames.sessions.repositories.SessionRepository;
import com.motomutterers.boardgames.teams.services.TeamUtilityService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final SessionUtilitysService sessionUtilitysService;
    private final RoomsUtilityService roomsUtilityService;
    private final TeamUtilityService teamUtilityService;
    private final UserService userService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    public SessionService(
        SessionRepository sessionRepository,
        SessionUtilitysService sessionUtilitysService,
        RoomsUtilityService roomsUtilityService,
        TeamUtilityService teamUtilityService,
        UserService userService
    ) {
        this.sessionRepository = sessionRepository;
        this.roomsUtilityService = roomsUtilityService;
        this.teamUtilityService = teamUtilityService;
        this.userService = userService;
        this.sessionUtilitysService = sessionUtilitysService;
    }

    public SessionResponse createSession(CreateSessionRequest request, Authentication authentication){
        String roomName = request.getRoomName();

        logger.info("Creating session for room " + roomName + " from user " + authentication.getName());
        User user = userService.getAuthenticatedUser(authentication);
        Room room = roomsUtilityService.getRoomByName(roomName);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);
        sessionUtilitysService.throwIfSessionExistsInRoom(room);

        if(!room.getStatus().equals(RoomStatus.WAITING)) {
            throw new BadActionException("Room is not in an adecuate state to start a session");
        }

        Session session = new Session(room);
        sessionRepository.save(session);

        roomsUtilityService.changeRoomStatus(room, RoomStatus.IN_PROGRESS);
        
        // Currently just hardcodes one person per team since skull-king doesn't have teams
        for(RoomUser player : room.getPlayers()){
            teamUtilityService.createTeam(List.of(player), session, player.getDisplayName());
        }

        return new SessionResponse(session);
    }
}
