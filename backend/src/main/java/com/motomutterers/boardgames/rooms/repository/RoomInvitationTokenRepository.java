package com.motomutterers.boardgames.rooms.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;

public interface RoomInvitationTokenRepository extends JpaRepository<RoomInvitationToken, UUID> {
    Optional<RoomInvitationToken> findByToken(String token);

    // The outstanding token for a RoomUser (at most one — enforced by a unique
    // constraint). Used to replace a token on re-invite and to accept/decline.
    Optional<RoomInvitationToken> findByRoomUser(RoomUser roomUser);

    // Every token in a room — used to clear outstanding invites when a game starts
    // or the room ends, so no one can join late.
    List<RoomInvitationToken> findAllByRoom(Room room);
}
