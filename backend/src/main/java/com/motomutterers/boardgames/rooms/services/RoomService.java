package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.models.VerificationToken;
import com.motomutterers.boardgames.auth.repositories.VerificationTokenRepository;
import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.email.EmailTemplates;
import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.exceptions.ValidationBuilder;
import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomInvitationRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.transaction.Transactional;

import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final GameService gameService;
    private final UserService userService;
    private final EmailService emailService;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;

    @Value("${room-invitation.expiration}")
    private int roomInvitationExpiration;

    @Value("app.base-url")
    private String baseUrl;

    public RoomService(
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        GameService gameService,
        UserService userService,
        RoomInvitationTokenRepository roomInvitationTokenRepository,
        EmailService emailService
    ){
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.gameService = gameService;
        this.userService = userService;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
        this.emailService = emailService;
    }

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public Room getRoomByName(String name){
        return roomRepository.findByName(name)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
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

    private void throwIsUserInActiveRoom(User user){
        if(getIsUserInActiveRoom(user)) {
            throw new UserInActiveRoomException("User is in an active room");
        }
    }

    public boolean getIsUserInActiveRoom(User user){
        return roomUserRepository.isUserInActiveRoom(user, List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS));
    }

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request, UUID userId){
        logger.info("Creating room with request: {}", request.toString());
        
        RoomResponse response;

        Game game = gameService.getGameByName(request.getGameName());
        User user = userService.getUserById(userId);
        throwIsUserInActiveRoom(user);
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
    public String invitePlayer(RoomInvitationRequest request) {
        String username = request.getUsername();
        String roomName = request.getRoomName();

        logger.info("Starting invite for: " + username);

        User user = userService.getUserByUsername(username);
        Room room = getRoomByName(roomName);

        new ValidationBuilder()
            .addError(getIsUserInActiveRoom(user), "inGame", "User is in another active session")
            .addError(!user.isActive(), "verified", "User needs to verify their account")
            .addError(  roomInvitationTokenRepository.isUserAlreadyInvitedAndNotExpired(user, room, InvitationStatus.PENDING, LocalDateTime.now()), 
                        "invited", "User is already invited")
            .validate();


        RoomInvitationToken roomInvitationToken = new RoomInvitationToken(
                                                    user, 
                                                    room,
                                                    UUID.randomUUID().toString(), 
                                                    LocalDateTime.now().plusSeconds(roomInvitationExpiration));
        
        roomInvitationTokenRepository.save(roomInvitationToken);
        String invitationLink = baseUrl + "/api/rooms/accept?token=" + roomInvitationToken.getToken();
        String html = EmailTemplates.roomInvitationEmail(user.getUsername(), invitationLink);
        emailService.sendEmail(user.getEmail(), "You've been invited to join a game!", html);
        return "Invited";
    }

    public RoomResponse getRoom(String name) {
        logger.info("Getting room with name: {}", name);

        RoomResponse response;

        Room room = getRoomByName(name);
        response = new RoomResponse(room);

        return response;
    }
}
