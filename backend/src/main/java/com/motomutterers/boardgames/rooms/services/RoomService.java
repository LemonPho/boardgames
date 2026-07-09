package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.email.EmailTemplates;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.notifications.dto.CreateRoomInvitationNotificationRequest;
import com.motomutterers.boardgames.notifications.services.NotificationService;
import com.motomutterers.boardgames.rooms.dto.CreateAnonymousPlayerRequest;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RemovePlayerRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenCancelledException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenExpiredException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenUsedException;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
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
    private final NotificationService notificationService;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        EmailService emailService,
        NotificationService notificationService
    ){
        this.roomsUtilityService = roomsUtilityService;
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.gameService = gameService;
        this.userService = userService;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
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

    @Transactional
    public void cancelRoom(String roomName, UUID userId){
        logger.info("Starting cancel room {} from user {}", roomName, userId);

        User user = userService.getUserById(userId);
        Room room = roomsUtilityService.getRoomByName(roomName);
        logger.info("Players: {}", room.getPlayers().toString());
        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        roomsUtilityService.cancelRoom(room);


        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    @Transactional
    public void leaveRoom(String roomName, UUID userId){
        logger.info("Starting leave room {} for user {}", roomName, userId);

        User user = userService.getUserById(userId);
        Room room = roomsUtilityService.getRoomByName(roomName);

        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        roomUserRepository.delete(roomUser);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    @Transactional
    public void createAnonymousPlayer(CreateAnonymousPlayerRequest request){
        User user = userService.getUserById(request.getAdminId());
        Room room = roomsUtilityService.getRoomByName(request.getRoomName());

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        RoomUser anonymous = new RoomUser(request.getDisplayName(), room);
        roomUserRepository.save(anonymous);

        roomsUtilityService.updateRoomLastUpdated(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }


    @Transactional
    public void removePlayer(RemovePlayerRequest request){
        User user = userService.getUserById(request.getAdminId());
        Room room = roomsUtilityService.getRoomByName(request.getRoomName());

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        RoomUser player = roomsUtilityService.getOrThrowRoomUserById(request.getRoomUserId());
        roomUserRepository.delete(player);

        roomsUtilityService.updateRoomLastUpdated(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    public List<UserAvailabilityResponse> searchUsersAvailability(String username, String roomName) {
        List<User> users = userService.findAllUserContainingUsername(username);
        Room room = roomsUtilityService.getRoomByName(roomName);

        return roomsUtilityService.getOccupiedUsers(users, room);
    }


    @Transactional
    public void invitePlayer(RoomInvitationRequest request) {
        User roomAdmin = userService.getUserById(request.getAdminId());
        
        String username = request.getUsername();
        String roomName = request.getRoomName();

        logger.info("Starting invite for: " + username);

        User user = userService.getUserByUsername(username);
        Room room = roomsUtilityService.getRoomByName(roomName);

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, roomAdmin);

        if(roomsUtilityService.getIsUserInActiveRoom(user)) throw new BadActionException("User is in an active session");
        if(!user.isActive()) throw new BadActionException("User needs to verify their email to be able to play");
        if(roomInvitationTokenRepository.isUserAlreadyInvitedAndNotExpired(user, room, InvitationStatus.PENDING, LocalDateTime.now())) {
            throw new BadActionException("User is already invited");
        }

        Optional<RoomInvitationToken> invitationResult = roomInvitationTokenRepository.findByRoomAndUser(room, user);
        if(invitationResult.isPresent()){
            RoomInvitationToken currentInvitation = invitationResult.get();
            roomInvitationTokenRepository.delete(currentInvitation);
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
        
        CreateRoomInvitationNotificationRequest notificationRequest = new CreateRoomInvitationNotificationRequest(user, room, roomAdmin);
        notificationService.createRoomInvitationNotification(notificationRequest);

        roomsUtilityService.updateRoomLastUpdated(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    @Transactional
    public void revokeInvite(RoomInvitationRequest request){
        User roomAdmin = userService.getUserById(request.getAdminId());
        
        String username = request.getUsername();
        String roomName = request.getRoomName();

        logger.info("Starting revoke invite for: " + username);

        User user = userService.getUserByUsername(username);
        Room room = roomsUtilityService.getRoomByName(roomName);

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, roomAdmin);

        RoomInvitationToken invite = roomsUtilityService.getOrThrowRoomInvitationTokenByRoomAndUser(room, user);
        invite.setStatus(InvitationStatus.CANCELLED);
        roomInvitationTokenRepository.save(invite);

        roomsUtilityService.updateRoomLastUpdated(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    @Transactional
    public RoomResponse acceptInvite(String token, Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        RoomInvitationToken roomInvitationToken = roomsUtilityService.getRoomInvitationTokenByToken(token);
    
        User authenticatedUser = userService.getUserById(userId);
        Room room = roomInvitationToken.getRoom();
        User invitationUser = roomInvitationToken.getUser();

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        if(!authenticatedUser.getId().equals(invitationUser.getId())){
            throw new BadActionException("A user cannot accept the invitation of another user for them!");
        }

        if(roomInvitationToken.getExpiresAt().isBefore(LocalDateTime.now().minusSeconds(roomInvitationExpiration))){
            roomInvitationTokenRepository.delete(roomInvitationToken);
            throw new RoomInvitationTokenExpiredException("This token is expired, please request a new invitation");
        }

        if(roomInvitationToken.getStatus().equals(InvitationStatus.CANCELLED)){
            throw new RoomInvitationTokenCancelledException("The invitation was cancelled");
        }

        if(roomInvitationToken.getStatus().equals(InvitationStatus.USED)){
            throw new RoomInvitationTokenUsedException("The invitation was already used, ask for a new one");
        }

        RoomUser roomUser = new RoomUser(invitationUser, room, RoomUserRoles.PLAYER);
        roomUserRepository.save(roomUser);
        roomInvitationToken.setStatus(InvitationStatus.USED);
        roomInvitationTokenRepository.save(roomInvitationToken);

        roomsUtilityService.updateRoomLastUpdated(room);
        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));

        return new RoomResponse(room);
    }

    @Transactional
    public RoomResponse getRoom(String name) {
        logger.info("Getting room with name: {}", name);

        RoomResponse response;

        Room room = roomsUtilityService.getRoomByName(name);

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        List<RoomInvitationToken> invitations = roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING);

        response = new RoomResponse(room, invitations);

        return response;
    }
}
