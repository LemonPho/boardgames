package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

@Service
public class RoomsUtilityService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;
    private final UserService userService;

    @Value("${room.waiting.expiration}")
    private int roomWaitingExpiration;

    public RoomsUtilityService(
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        RoomInvitationTokenRepository roomInvitationTokenRepository,
        UserService userService
    ) {
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
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

    public RoomInvitationToken getRoomInvitationTokenByToken(String token){
        return roomInvitationTokenRepository.findByToken(token)
            .orElseThrow(() -> new RoomInvitationTokenNotFoundException("That invitation no longer exists"));
    }

    public boolean isRoomExpired(Room room){
        if(room.getStatus() == RoomStatus.WAITING && room.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(roomWaitingExpiration))){
            return true;
        } else {
            return false;
        }
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void expireRoom(Room room){
        room.setStatus(RoomStatus.CANCELLED);
        roomRepository.save(room);
    }

    public List<UserAvailabilityResponse> getOccupiedUsers(List<User> users, Room currentRoom){
        Set<Room> rooms = roomUserRepository.findRoomsByUsersAndStatuses(users, List.of(RoomStatus.WAITING));
        rooms.stream()
            .filter(room -> isRoomExpired(room))
            .forEach(this::expireRoom);

        Set<User> occupiedUsers = roomUserRepository.findOccupiedUsers(
            users,
            List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS));
            
        Set<User> invitedUsers = roomInvitationTokenRepository.findInvitedToRoom(
            users,
            currentRoom,
            InvitationStatus.PENDING,
            LocalDateTime.now()
        );

        return users.stream()
            .map(u -> new UserAvailabilityResponse(u, occupiedUsers.contains(u), invitedUsers.contains(u)))
            .toList();
    }

    public void throwIsUserInActiveRoom(User user){
        if(getIsUserInActiveRoom(user)) {
            throw new UserInActiveRoomException("User is in an active room");
        }
    }

    

    public boolean getIsUserInActiveRoom(User user){
        Optional<Room> result = roomUserRepository.findActiveRoomByUser(user, List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS));
        if(result.isEmpty()) return false;
        Room room = result.get();
        if(isRoomExpired(room)){
            room.setStatus(RoomStatus.CANCELLED);
            roomRepository.save(room);
            return false;
        }
        return true;
    }
    
}
