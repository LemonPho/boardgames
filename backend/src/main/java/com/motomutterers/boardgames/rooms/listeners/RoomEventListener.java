package com.motomutterers.boardgames.rooms.listeners;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.rooms.dto.RoomResponse;
import com.motomutterers.boardgames.rooms.events.RoomUpdatedEvent;
import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;

@Component
public class RoomEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomsUtilityService roomsUtilityService;

    public RoomEventListener(SimpMessagingTemplate messagingTemplate, RoomsUtilityService roomsUtilityService) {
        this.messagingTemplate = messagingTemplate;
        this.roomsUtilityService = roomsUtilityService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleRoomUpdated(RoomUpdatedEvent event) {
        Room room = roomsUtilityService.getRoomByName(event.getRoomName());
        // Players carry their own invite state (PENDING_INVITE/DECLINED), so the
        // room snapshot is self-contained.
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getName(), new RoomResponse(room));
    }
}