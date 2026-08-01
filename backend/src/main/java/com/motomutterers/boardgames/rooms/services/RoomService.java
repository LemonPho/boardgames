package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import com.motomutterers.boardgames.rooms.dto.MovePlayerRequest;
import com.motomutterers.boardgames.rooms.dto.RemovePlayerRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenCancelledException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenExpiredException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenUsedException;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomConfiguration;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus;
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
        RoomConfiguration configuration = request.getConfiguration() != null
            ? request.getConfiguration()
            : new RoomConfiguration();
        Room room = new Room(game, roomName, configuration);
        // The admin is the first seat (position 0) and always ACTIVE.
        RoomUser roomUser = new RoomUser(user, room, RoomUserRoles.ADMIN, 0);
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
        roomsUtilityService.throwIfRoomIsFull(room);

        RoomUser anonymous = new RoomUser(
            request.getDisplayName(), room, roomsUtilityService.nextPlayingPosition(room));
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

    /**
     * Move a player to a new seat/turn position. The moved player lands at
     * newLocation and everyone else keeps their relative order, with positions
     * renumbered contiguously (0..n-1) so the ordering can't drift. This order is
     * what the game uses for the first-round leader and round rotation.
     */
    @Transactional
    public void movePlayer(MovePlayerRequest request){
        User user = userService.getUserById(request.getAdminId());
        Room room = roomsUtilityService.getRoomByName(request.getRoomName());

        roomsUtilityService.throwIfUserIsNotRoomAdmin(room, user);

        RoomUser moving = roomsUtilityService.getOrThrowRoomUserById(request.getRoomUserId());
        if(moving.getRoom() == null || !moving.getRoom().getId().equals(room.getId())){
            throw new BadActionException("That player is not in this room");
        }

        // Work on the room's players in current order, pull out the mover, and
        // reinsert at the requested slot (clamped to the valid range).
        List<RoomUser> ordered = new ArrayList<>(room.getPlayers());
        ordered.sort(Comparator.comparingInt(RoomUser::getPlayingPosition));
        ordered.removeIf(ru -> ru.getId().equals(moving.getId()));

        int target = Math.max(0, Math.min(request.getNewLocation(), ordered.size()));
        ordered.add(target, moving);

        // Renumber everyone so positions stay contiguous, only saving rows that moved.
        for(int i = 0; i < ordered.size(); i++){
            RoomUser ru = ordered.get(i);
            if(ru.getPlayingPosition() != i){
                ru.setPlayingPosition(i);
                roomUserRepository.save(ru);
            }
        }

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
        roomsUtilityService.throwIfRoomIsFull(room);

        if(roomsUtilityService.getIsUserInActiveRoom(user)) throw new BadActionException("User is in an active session");
        if(!user.isActive()) throw new BadActionException("User needs to verify their email to be able to play");

        // The user's standing in THIS room decides whether we can invite: already
        // playing or already invited is a no-op; a prior decline blocks re-inviting
        // (anti-spam). Only a user with no RoomUser here can be freshly invited.
        Optional<RoomUserStatus> existingStatus = roomsUtilityService.getRoomUserStatus(room, user);
        if(existingStatus.isPresent()){
            switch(existingStatus.get()){
                case ACTIVE -> throw new BadActionException("User is already in the room");
                case PENDING_INVITE -> throw new BadActionException("User is already invited");
                case DECLINED -> throw new BadActionException("User has declined an invite to this room");
            }
        }

        // Create the invited player's RoomUser up front (PENDING_INVITE), so their
        // seat/turn position is fixed at invite time and the game's ordering is
        // known before they even accept.
        RoomUser roomUser = new RoomUser(
            user, room, RoomUserStatus.PENDING_INVITE, roomsUtilityService.nextPlayingPosition(room));
        roomUserRepository.save(roomUser);

        RoomInvitationToken roomInvitationToken = new RoomInvitationToken(
            roomUser,
            room,
            UUID.randomUUID().toString(),
            LocalDateTime.now().plusSeconds(roomInvitationExpiration));

        room.setLastUpdated(LocalDateTime.now());
        roomRepository.save(room);
        roomInvitationTokenRepository.save(roomInvitationToken);

        String invitationLink = frontendBaseUrl + "rooms/accept?token=" + roomInvitationToken.getToken();
        String html = EmailTemplates.roomInvitationEmail(user.getUsername(), invitationLink);
        emailService.sendEmail(user.getEmail(), "You've been invited to join a game!", html);
        
        CreateRoomInvitationNotificationRequest notificationRequest = new CreateRoomInvitationNotificationRequest(user, room, roomAdmin, roomInvitationToken.getToken());
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

        RoomUser roomUser = roomsUtilityService.getOrThrowRoomUserByUserAndRoom(user, room);
        RoomInvitationToken invite = roomsUtilityService.getOrThrowRoomInvitationTokenByRoomUser(roomUser);
        String inviteToken = invite.getToken();

        // Revoking removes the invited player entirely — deleting the RoomUser
        // cascades to its token, so the seat is freed and they can be re-invited.
        roomUserRepository.delete(roomUser);

        // The invite is revoked, so its notification is gone for good — delete it
        // (and push a live removal to the invited user's open client).
        notificationService.deleteInvitation(inviteToken);

        roomsUtilityService.updateRoomLastUpdated(room);

        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    @Transactional
    public RoomResponse acceptInvite(String token, Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        RoomInvitationToken roomInvitationToken = roomsUtilityService.getRoomInvitationTokenByToken(token);
    
        User authenticatedUser = userService.getUserById(userId);
        Room room = roomInvitationToken.getRoom();
        RoomUser roomUser = roomInvitationToken.getRoomUser();
        User invitationUser = roomUser.getUser();

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        if(!authenticatedUser.getId().equals(invitationUser.getId())){
            throw new BadActionException("A user cannot accept the invitation of another user for them!");
        }

        if(roomInvitationToken.getExpiresAt().isBefore(LocalDateTime.now().minusSeconds(roomInvitationExpiration))){
            // Expired: drop the invited player entirely (cascades to the token).
            roomUserRepository.delete(roomUser);
            throw new RoomInvitationTokenExpiredException("This token is expired, please request a new invitation");
        }

        roomsUtilityService.throwIfPlayerLimitReached(room);

        // Convert the reserved seat into a joined player, keeping its position, and
        // delete the token so there's no lingering invitation backlog.
        roomUser.setStatus(RoomUserStatus.ACTIVE);
        roomUserRepository.save(roomUser);
        roomInvitationTokenRepository.delete(roomInvitationToken);

        // The invite has been accepted, so its notification is no longer
        // actionable — mark it read so it stops lingering as unread.
        notificationService.markInvitationRead(token);

        roomsUtilityService.updateRoomLastUpdated(room);
        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));

        return new RoomResponse(room);
    }

    // Decline an invite: mark the RoomUser DECLINED (retained so the admin can't
    // re-invite/spam) and delete the token. The seat is released from the active
    // count but the row stays as a record of the decline.
    @Transactional
    public void declineInvite(String token, Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        RoomInvitationToken roomInvitationToken = roomsUtilityService.getRoomInvitationTokenByToken(token);

        RoomUser roomUser = roomInvitationToken.getRoomUser();
        Room room = roomInvitationToken.getRoom();

        if(!roomUser.getUser().getId().equals(userId)){
            throw new BadActionException("A user cannot decline the invitation of another user for them!");
        }

        roomUser.setStatus(RoomUserStatus.DECLINED);
        roomUserRepository.save(roomUser);
        roomInvitationTokenRepository.delete(roomInvitationToken);

        // The invite is no longer actionable — dismiss its notification live.
        notificationService.deleteInvitation(token);

        roomsUtilityService.updateRoomLastUpdated(room);
        eventPublisher.publishEvent(new RoomUpdatedEvent(room.getName()));
    }

    // The room the user is currently in (WAITING or IN_PROGRESS), or null if none.
    // Backs the home page's rooms section so a returning user is shown their room.
    @Transactional
    public RoomResponse getActiveRoom(UUID userId) {
        User user = userService.getUserById(userId);
        return roomsUtilityService.getActiveRoom(user)
            .map(RoomResponse::new)
            .orElse(null);
    }

    @Transactional
    public RoomResponse getRoom(String name) {
        logger.info("Getting room with name: {}", name);

        RoomResponse response;

        Room room = roomsUtilityService.getRoomByName(name);

        if(roomsUtilityService.isRoomExpired(room)) roomsUtilityService.cancelRoom(room);

        // Players now include pending invites (as PENDING_INVITE RoomUsers), so the
        // response carries invite state on each player — no separate invite list.
        response = new RoomResponse(room);

        return response;
    }
}
