package com.motomutterers.boardgames.rooms.services;

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
import com.motomutterers.boardgames.rooms.services.RoomService;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomUserRepository roomUserRepository;
    @Mock private GameService gameService;
    @Mock private UserService userService;

    @InjectMocks
    private RoomService roomService;

    // helpers
    private User mockUser() {
        return new User("test@test.com", "testuser", "hash");
    }

    private Game mockGame() {
        Game game = new Game();
        game.setName("Skull King");
        return game;
    }

    @Test
    void getRoomById_roomExists_returnsRoom() {
        UUID id = UUID.randomUUID();
        Room room = new Room(mockGame(), "testuser's Skull King Room", TrackingMode.ADMIN);
        when(roomRepository.findById(id)).thenReturn(Optional.of(room));

        Room result = roomService.getRoomById(id);

        assertEquals(room, result);
    }

    @Test
    void getRoomById_roomNotFound_throwsRoomNotFoundException() {
        UUID id = UUID.randomUUID();
        when(roomRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomById(id));
    }

    @Test
    void getRoomByName_roomExists_returnsRoom() {
        Room room = new Room(mockGame(), "testuser's Skull King Room", TrackingMode.ADMIN);
        when(roomRepository.findByName("testuser's Skull King Room")).thenReturn(Optional.of(room));

        Room result = roomService.getRoomByName("testuser's Skull King Room");

        assertEquals(room, result);
    }

    @Test
    void getRoomByName_roomNotFound_throwsRoomNotFoundException() {
        when(roomRepository.findByName("unknown")).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomByName("unknown"));
    }

    @Test
    void createRoom_baseName_notTaken_usesBaseName() {
        UUID userId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("Skull King", TrackingMode.ADMIN);
        User user = mockUser();
        Game game = mockGame();

        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(game);
        when(roomRepository.existsByName("testuser's Skull King Room")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomUserRepository.save(any(RoomUser.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.createRoom(request, userId);

        assertEquals("testuser's Skull King Room", response.getName());
    }

    @Test
    void createRoom_baseName_taken_appendsSuffix() {
        UUID userId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("Skull King", TrackingMode.ADMIN);
        User user = mockUser();
        Game game = mockGame();

        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(game);
        when(roomRepository.existsByName("testuser's Skull King Room")).thenReturn(true);
        when(roomRepository.existsByName("testuser's Skull King Room #2")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomUserRepository.save(any(RoomUser.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.createRoom(request, userId);

        assertEquals("testuser's Skull King Room #2", response.getName());
    }

    @Test
    void createRoom_savesRoomAndRoomUser() {
        UUID userId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("Skull King", TrackingMode.ADMIN);
        User user = mockUser();
        Game game = mockGame();

        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(game);
        when(roomRepository.existsByName(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomUserRepository.save(any(RoomUser.class))).thenAnswer(i -> i.getArgument(0));

        roomService.createRoom(request, userId);

        verify(roomRepository).save(any(Room.class));
        verify(roomUserRepository).save(any(RoomUser.class));
    }

    @Test
    void createRoom_adminIsAddedAsPlayer() {
        UUID userId = UUID.randomUUID();
        CreateRoomRequest request = new CreateRoomRequest("Skull King", TrackingMode.ADMIN);
        User user = mockUser();
        Game game = mockGame();

        when(userService.getUserById(userId)).thenReturn(user);
        when(gameService.getGameByName("Skull King")).thenReturn(game);
        when(roomRepository.existsByName(any())).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
        when(roomUserRepository.save(any(RoomUser.class))).thenAnswer(i -> i.getArgument(0));

        roomService.createRoom(request, userId);

        verify(roomUserRepository).save(argThat(roomUser ->
            roomUser.getRole() == RoomUserRoles.ADMIN &&
            roomUser.getUser().equals(user)
        ));
    }
}