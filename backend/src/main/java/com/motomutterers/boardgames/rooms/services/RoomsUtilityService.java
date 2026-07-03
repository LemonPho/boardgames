package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomUserNotFoundException;
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
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;

@Service
public class RoomsUtilityService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;

    @Value("${room.waiting.expiration}")
    private int roomWaitingExpiration;

    public RoomsUtilityService(
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        RoomInvitationTokenRepository roomInvitationTokenRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
    }

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public Room getRoomByName(String name){
        return roomRepository.findByName(name)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public RoomUser getOrThrowRoomUserByDisplayNameAndRoom(String displayName, Room room){
        return roomUserRepository.findByDisplayNameAndRoom(displayName, room)
            .orElseThrow(() -> new RoomUserNotFoundException("User " + displayName + " not found in room " + room.getName()));
    }

    public RoomInvitationToken getRoomInvitationTokenByToken(String token){
        return roomInvitationTokenRepository.findByToken(token)
            .orElseThrow(() -> new RoomInvitationTokenNotFoundException("That invitation no longer exists"));
    }

    public RoomInvitationToken getOrThrowRoomInvitationTokenByRoomAndUser(Room room, User user){
        return roomInvitationTokenRepository.findByRoomAndUser(room, user)
            .orElseThrow(() -> new RoomInvitationTokenNotFoundException("The invitation no longer exists"));
    }

    public boolean isRoomExpired(Room room){
        if(room.getStatus() == RoomStatus.WAITING && room.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(roomWaitingExpiration))){
            return true;
        } else {
            return false;
        }
    }

    public void throwIfUserIsNotRoomAdmin(Room room, User user){
        Optional<RoomUser> result = room.getPlayers()
            .stream()
            .filter(p -> p.getRole().equals(RoomUserRoles.ADMIN))
            .filter(p -> p.getUser().getId().equals(user.getId()))
            .findFirst();

        if(result.isEmpty()) throw new BadActionException("You need to be the room admin to perform this action.");
    }

    public void throwIfUserNotInRoom(Room room, User user){
        roomUserRepository.findByDisplayNameAndRoom(user.getUsername(), room)
            .orElseThrow(() -> new BadActionException("User not in room"));
    }

    public void throwIfDisplayNameAlreadyExistsInRoom(String displayName, Room room){
        Optional<RoomUser> result = room.getPlayers()
            .stream()
            .filter(p -> p.getRole().equals(RoomUserRoles.ANONYMOUS))
            .filter(p -> p.getDisplayName().equals(displayName))
            .findFirst();

        if(result.isPresent()) throw new BadActionException("Each player needs a unique name");
    }

    public RoomUser renameAnonymousPlayer(RoomUser anonymous, Room room){
        AtomicInteger i = new AtomicInteger(1);
        while(i.get() < 100){
            Optional<RoomUser> result = room.getPlayers()
            .stream()
            .filter(p -> p.getRole().equals(RoomUserRoles.ANONYMOUS))
            .filter(p -> p.getDisplayName().equals(anonymous.getDisplayName() + " (" + i.get() + ")"))
            .findFirst();

            if(result.isEmpty()){
                anonymous.setDisplayName(anonymous.getDisplayName() + " (" + i.get() + ")");
                roomUserRepository.save(anonymous);
                return anonymous;
            }
            i.incrementAndGet();
        }

        throw new BadActionException("There are no available anonymous names for: " + anonymous.getDisplayName());

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
            
        Set<User> invitedUsers = roomInvitationTokenRepository.findUsersInvitedToRoom(
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

    public void updateRoomLastUpdated(Room room){
        room.setLastUpdated(LocalDateTime.now());
        roomRepository.save(room);
    }

    public void cancelRoom(Room room){
        room.setStatus(RoomStatus.CANCELLED);
        List<RoomInvitationToken> invitations = roomInvitationTokenRepository.findRoomInvitations(room);
        for(RoomInvitationToken invitation : invitations){
            invitation.setStatus(InvitationStatus.CANCELLED);
        }

        roomInvitationTokenRepository.saveAll(invitations);
        roomRepository.save(room);
    }
    
}
