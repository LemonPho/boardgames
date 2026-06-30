package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.email.EmailTemplates;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.exceptions.RoomExpiredException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenExpiredException;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomsUtilityService roomsUtilityService;
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final GameService gameService;
    private final UserService userService;
    private final EmailService emailService;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;

    @Value("${room-invitation.expiration}")
    private int roomInvitationExpiration;

    @Value("${room.waiting.expiration}")
    private int roomWaitingExpiration;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public RoomService(
        RoomsUtilityService roomsUtilityService,
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        GameService gameService,
        UserService userService,
        RoomInvitationTokenRepository roomInvitationTokenRepository,
        EmailService emailService
    ){
        this.roomsUtilityService = roomsUtilityService;
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.gameService = gameService;
        this.userService = userService;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
        this.emailService = emailService;
    }

    private String generateRoomName(String username, String gameName) {
        String baseName = username + "'s " + gameName + " Room";
        
        if (!roomRepository.existsByName(baseName)) {
            return baseName;
        }

        int suffix = 2;
        while (true) {
            String candidate = baseName + " " + suffix;
            if (!roomRepository.existsByName(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, UUID userId){
        logger.info("Creating room with request: {}", request.toString());
        
        RoomResponse response;

        Game game = gameService.getGameByName(request.getGameName());
        User user = userService.getUserById(userId);
        roomsUtilityService.throwIsUserInActiveRoom(user);
        String roomName = generateRoomName(user.getUsername(), game.getName());
        Room room = new Room(game, roomName, request.getTrackingMode());
        RoomUser roomUser = new RoomUser(user, room, RoomUserRoles.ADMIN);
        room.addPlayer(roomUser);

        roomRepository.save(room);
        roomUserRepository.save(roomUser);

        response = new RoomResponse(room);

        return response;
    }

    public List<UserAvailabilityResponse> searchUsersAvailability(String username, String roomName) {
        List<User> users = userService.findAllUserContainingUsername(username);
        Room room = roomsUtilityService.getRoomByName(roomName);

        return roomsUtilityService.getOccupiedUsers(users, room);
    }


    @Transactional
    public String invitePlayer(RoomInvitationRequest request) {
        String username = request.getUsername();
        String roomName = request.getRoomName();

        logger.info("Starting invite for: " + username);

        User user = userService.getUserByUsername(username);
        Room room = roomsUtilityService.getRoomByName(roomName);
        if(roomsUtilityService.getIsUserInActiveRoom(user)) throw new BadActionException("User is in an active session");
        if(!user.isActive()) throw new BadActionException("User needs to verify their email to be able to play");
        if(roomInvitationTokenRepository.isUserAlreadyInvitedAndNotExpired(user, room, InvitationStatus.PENDING, LocalDateTime.now())) {
            throw new BadActionException("User is already invited");
        }

        RoomInvitationToken roomInvitationToken = new RoomInvitationToken(
            user, 
            room,
            UUID.randomUUID().toString(), 
            LocalDateTime.now().plusSeconds(roomInvitationExpiration));

        room.setLastUpdated(LocalDateTime.now());
        roomRepository.save(room);
        roomInvitationTokenRepository.save(roomInvitationToken);

        String invitationLink = frontendBaseUrl + "rooms/accept?token=" + roomInvitationToken.getToken();
        String html = EmailTemplates.roomInvitationEmail(user.getUsername(), invitationLink);
        emailService.sendEmail(user.getEmail(), "You've been invited to join a game!", html);
        return "Invited";
    }

    @Transactional
    public String acceptInvite(String token, Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        RoomInvitationToken roomInvitationToken = roomsUtilityService.getRoomInvitationTokenByToken(token);
    
        User authenticatedUser = userService.getUserById(userId);
        Room room = roomInvitationToken.getRoom();
        User invitationUser = roomInvitationToken.getUser();

        if(authenticatedUser.getId() != invitationUser.getId()){
            throw new BadActionException("A user cannot accept the invitation of another user for them!");
        }

        if(roomInvitationToken.getExpiresAt().isBefore(LocalDateTime.now().minusSeconds(roomInvitationExpiration))){
            roomInvitationTokenRepository.delete(roomInvitationToken);
            throw new RoomInvitationTokenExpiredException("This token is expired, please request a new invitation");
        }

        RoomUser roomUser = new RoomUser(invitationUser, room, RoomUserRoles.PLAYER);
        roomUserRepository.save(roomUser);
        roomInvitationTokenRepository.delete(roomInvitationToken);

        return room.getName();
    }

    @Transactional
    public RoomResponse getRoom(String name) {
        logger.info("Getting room with name: {}", name);

        RoomResponse response;

        Room room = roomsUtilityService.getRoomByName(name);

        if(room.getStatus() == RoomStatus.WAITING && room.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(roomWaitingExpiration))){
            roomsUtilityService.expireRoom(room);
            throw new RoomExpiredException("Room expires after 15 minutes of inactivity");
        }

        response = new RoomResponse(room);

        return response;
    }
}
