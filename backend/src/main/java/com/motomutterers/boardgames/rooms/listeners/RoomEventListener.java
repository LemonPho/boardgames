package com.motomutterers.boardgames.rooms.listeners;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.model.Invitation.InvitationStatus;
import com.motomutterers.boardgames.rooms.model.Invitation.RoomInvitationToken;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.repository.RoomInvitationTokenRepository;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;

@Component
public class RoomEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomsUtilityService roomsUtilityService;
    private final RoomInvitationTokenRepository roomInvitationTokenRepository;

    public RoomEventListener(SimpMessagingTemplate messagingTemplate, RoomsUtilityService roomsUtilityService, RoomInvitationTokenRepository roomInvitationTokenRepository) {
        this.messagingTemplate = messagingTemplate;
        this.roomsUtilityService = roomsUtilityService;
        this.roomInvitationTokenRepository = roomInvitationTokenRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoomUpdated(RoomUpdatedEvent event) {
        Room room = roomsUtilityService.getRoomByName(event.getRoomName());
        List<RoomInvitationToken> invitations = roomInvitationTokenRepository.findAllByRoomAndStatus(room, InvitationStatus.PENDING);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getName(), new RoomResponse(room, invitations));
    }
}