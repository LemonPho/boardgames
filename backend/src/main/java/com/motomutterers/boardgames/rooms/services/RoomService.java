package com.motomutterers.boardgames.rooms.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.games.model.Game;
import com.motomutterers.boardgames.games.services.GameService;
import com.motomutterers.boardgames.rooms.dto.CreateRoomRequest;
import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.model.Room;
import com.motomutterers.boardgames.rooms.model.RoomUser;
import com.motomutterers.boardgames.rooms.model.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.TrackingMode;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final GameService gameService;
    private final UserService userService;

    public RoomService(
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        GameService gameService,
        UserService userService
    ){
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.gameService = gameService;
        this.userService = userService;
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
            String candidate = baseName + " #" + suffix;
            if (!roomRepository.existsByName(candidate)) {
                return candidate;
            }
            suffix++;
        }
    }

    public RoomResponse createRoom(CreateRoomRequest request, UUID userId){
        RoomResponse response;

        Game game = gameService.getGameByName(request.getGameName());
        User user = userService.getUserById(userId);
        String roomName = generateRoomName(user.getUsername(), game.getName());
        Room room = new Room(game, roomName, request.getTrackingMode());
        RoomUser roomUser = new RoomUser(user, room, RoomUserRoles.ADMIN);
        room.addPlayer(roomUser);

        roomRepository.save(room);
        roomUserRepository.save(roomUser);

        response = new RoomResponse(room);

        return response;
    }
}
