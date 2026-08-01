package com.motomutterers.boardgames.rooms.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.motomutterers.boardgames.exceptions.BadActionException;
import com.motomutterers.boardgames.notifications.repositories.NotificationRepository;
import com.motomutterers.boardgames.rooms.exceptions.RoomExpiredException;
import com.motomutterers.boardgames.rooms.exceptions.RoomInvitationTokenNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomNotFoundException;
import com.motomutterers.boardgames.rooms.exceptions.RoomUserNotFoundException;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomStatus;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserStatus;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.repository.RoomRepository;
import com.motomutterers.boardgames.rooms.repository.RoomUserRepository;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.services.SessionUtilitysService;
import com.motomutterers.boardgames.user.dto.UserAvailabilityResponse;
import com.motomutterers.boardgames.user.exceptions.UserInActiveRoomException;
import com.motomutterers.boardgames.user.model.User;

@Service
public class RoomsUtilityService {
    private final RoomRepository roomRepository;
    private final RoomUserRepository roomUserRepository;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;
    private final SessionUtilitysService sessionUtilitysService;
    private final NotificationRepository notificationRepository;

    @Value("${room.waiting.expiration}")
    private int roomWaitingExpiration;

    @Value("${room.in-progress.expiration}")
    private int roomInProgressExpiration;

    public RoomsUtilityService(
        RoomRepository roomRepository,
        RoomUserRepository roomUserRepository,
        RoomInvitationTokenRepository roomInvitationTokenRepository,
        SessionUtilitysService sessionUtilitysService,
        NotificationRepository notificationRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomUserRepository = roomUserRepository;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
        this.sessionUtilitysService = sessionUtilitysService;
        this.notificationRepository = notificationRepository;
    }

    public Room getRoomById(UUID id){
        return roomRepository.findById(id)
            .orElseThrow(() -> new RoomNotFoundException("Room was not found"));
    }

    public Room getRoomByName(String name){
        Optional<Room> result = roomRepository.findByName(name);
        if(result.isEmpty()){
            throw new RoomNotFoundException("Room not found");
        }

        Room room = result.get();
        checkAndHandleExpiry(room);
        return room;
    }

    public void checkAndHandleExpiry(Room room) {
        if (isRoomExpired(room)) {
            cancelRoom(room);
            throw new RoomExpiredException("Session was cancelled due to room expiry");
        }
    }

    public RoomUser getOrThrowRoomUserById(UUID id){
        return roomUserRepository.findById(id)
            .orElseThrow(() -> new RoomUserNotFoundException("Player not found"));
    }

    public RoomUser getOrThrowRoomUserByUserAndRoom(User user, Room room){
        return roomUserRepository.findByUserAndRoom(user, room)
            .orElseThrow(() -> new RoomUserNotFoundException("User not in room"));
    }


    public RoomInvitationToken getRoomInvitationTokenByToken(String token){
        return roomInvitationTokenRepository.findByToken(token)
            .orElseThrow(() -> new RoomInvitationTokenNotFoundException("That invitation no longer exists"));
    }

    public RoomInvitationToken getOrThrowRoomInvitationTokenByRoomUser(RoomUser roomUser){
        return roomInvitationTokenRepository.findByRoomUser(roomUser)
            .orElseThrow(() -> new RoomInvitationTokenNotFoundException("The invitation no longer exists"));
    }

    public int nextPlayingPosition(Room room){
        Integer max = roomUserRepository.findMaxPlayingPosition(room);
        return max == null ? 0 : max + 1;
    }

    public Optional<RoomUser> findRoomUser(Room room, User user){
        return roomUserRepository.findByUserAndRoom(user, room);
    }

    public Optional<RoomUserStatus> getRoomUserStatus(Room room, User user){
        return findRoomUser(room, user).map(RoomUser::getStatus);
    }

    public boolean isRoomExpired(Room room){
        if(room.getStatus().equals(RoomStatus.COMPLETED) || room.getStatus().equals(RoomStatus.CANCELLED)){
            return false;
        }

        int expiration = room.getStatus().equals(RoomStatus.IN_PROGRESS)
            ? roomInProgressExpiration
            : roomWaitingExpiration;

        return room.getLastUpdated().isBefore(LocalDateTime.now().minusSeconds(expiration));
    }

    public void throwIfUserIsNotRoomAdmin(Room room, User user){
        Optional<RoomUser> result = room.getPlayers()
            .stream()
            .filter(p -> p.getRole().equals(RoomUserRoles.ADMIN))
            .filter(p -> p.getUser().getId().equals(user.getId()))
            .findFirst();

        if(result.isEmpty()) throw new BadActionException("You need to be the room admin to perform this action.");
    }

    /**
     * A room can hold at most game.maxPlayers seats. A pending invite reserves a
     * seat (its RoomUser is PENDING_INVITE) so we don't over-invite past the cap
     * once everyone accepts; declined rows don't occupy a seat. Used when adding
     * a new occupant (invite, anonymous player).
     */
    public void throwIfRoomIsFull(Room room){
        int maxPlayers = room.getGame().getMaxPlayers();
        long occupied = room.getPlayers().stream()
            .filter(p -> p.getStatus() != RoomUserStatus.DECLINED)
            .count();

        if(occupied >= maxPlayers){
            throw new BadActionException("Room is full");
        }
    }

    /**
     * Guards accepting an invite: counts only joined players (ACTIVE). The invite
     * being accepted is still PENDING_INVITE — its reserved seat is about to
     * convert to ACTIVE — so it must not be double-counted. Defends against
     * pre-existing over-invites and concurrent accepts.
     */
    public void throwIfPlayerLimitReached(Room room){
        int maxPlayers = room.getGame().getMaxPlayers();
        long active = room.getPlayers().stream()
            .filter(p -> p.getStatus() == RoomUserStatus.ACTIVE)
            .count();
        if(active >= maxPlayers){
            throw new BadActionException("Room is full");
        }
    }

    public void throwIfUserNotInRoom(Room room, User user){
        boolean found = room.getPlayers().stream()
            .filter(p -> p.getStatus() == RoomUserStatus.ACTIVE)
            .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(user.getId()));
        if(!found) throw new BadActionException("User not in room");
    }

    public void changeRoomStatus(Room room, RoomStatus roomStatus){
        room.setStatus(roomStatus);
        roomRepository.save(room);

        // Once a room reaches a terminal state, dismiss its pending invitations.
        if(roomStatus.equals(RoomStatus.COMPLETED) || roomStatus.equals(RoomStatus.CANCELLED)){
            notificationRepository.markReadByRoomName(room.getName());
        }
    }

    public List<UserAvailabilityResponse> getOccupiedUsers(List<User> users, Room currentRoom){
        Set<Room> rooms = roomUserRepository.findRoomsByUsersAndStatuses(
            users, List.of(RoomStatus.WAITING), RoomUserStatus.ACTIVE);
        rooms.stream()
            .filter(room -> isRoomExpired(room))
            .forEach(this::cancelRoom);

        // "In a game" = actively playing somewhere (ACTIVE); a pending/declined
        // invite elsewhere doesn't make a user unavailable to invite here.
        Set<User> occupiedUsers = roomUserRepository.findOccupiedUsers(
            users,
            List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS),
            RoomUserStatus.ACTIVE);

        // "invited" / "declined" are relative to THIS room, read from the user's
        // RoomUser status in it, so search shows their standing for this room.
        return users.stream()
            .map(u -> {
                RoomUserStatus status = getRoomUserStatus(currentRoom, u).orElse(null);
                boolean invited = status == RoomUserStatus.PENDING_INVITE;
                boolean declined = status == RoomUserStatus.DECLINED;
                return new UserAvailabilityResponse(u, occupiedUsers.contains(u), invited, declined);
            })
            .toList();
    }

    public void throwIsUserInActiveRoom(User user){
        if(getIsUserInActiveRoom(user)) {
            throw new UserInActiveRoomException("User is in an active room");
        }
    }

    public boolean getIsUserInActiveRoom(User user){
        return getActiveRoom(user).isPresent();
    }

    // The user's current WAITING/IN_PROGRESS room, if any. An expired room is
    // cancelled and treated as none, so callers never see a stale room.
    public Optional<Room> getActiveRoom(User user){
        Optional<Room> result = roomUserRepository.findActiveRoomByUser(
            user, List.of(RoomStatus.WAITING, RoomStatus.IN_PROGRESS), RoomUserStatus.ACTIVE);
        if(result.isEmpty()) return Optional.empty();
        Room room = result.get();
        if(isRoomExpired(room)){
            cancelRoom(room);
            return Optional.empty();
        }
        return Optional.of(room);
    }

    public void updateRoomLastUpdated(Room room){
        room.setLastUpdated(LocalDateTime.now());
        roomRepository.save(room);
    }

    /**
     * Clears outstanding invitations when a game starts: deletes all tokens and
     * removes any RoomUser that never became ACTIVE (still PENDING_INVITE, or a
     * DECLINED record). This stops a late accept from injecting a player into a
     * game in progress, and keeps only real players for team creation.
     */
    public void clearPendingInvitesForGameStart(Room room){
        roomInvitationTokenRepository.deleteAll(roomInvitationTokenRepository.findAllByRoom(room));

        List<RoomUser> notJoined = room.getPlayers().stream()
            .filter(p -> p.getStatus() != RoomUserStatus.ACTIVE)
            .toList();
        roomUserRepository.deleteAll(notJoined);
        room.getPlayers().removeAll(notJoined);
    }

    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void cancelRoom(Room room){
        room.setStatus(RoomStatus.CANCELLED);

        // The room is over, so any outstanding invites are no longer actionable —
        // delete the tokens (the PENDING_INVITE RoomUsers stay; a cancelled room
        // never starts a game, so they can't leak into play).
        roomInvitationTokenRepository.deleteAll(roomInvitationTokenRepository.findAllByRoom(room));

        roomRepository.save(room);

        // Dismiss the room's invitation notifications.
        notificationRepository.markReadByRoomName(room.getName());

        sessionUtilitysService.cancelSessionIfExists(room);
    }
    
}
