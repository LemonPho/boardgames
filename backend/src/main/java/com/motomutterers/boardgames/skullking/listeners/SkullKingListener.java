package com.motomutterers.boardgames.skullking.listeners;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.rooms.model.Room.RoomUser;
import com.motomutterers.boardgames.rooms.model.Room.RoomUserRoles;
import com.motomutterers.boardgames.rooms.services.RoomsUtilityService;
import com.motomutterers.boardgames.sessions.models.session.Session;
import com.motomutterers.boardgames.sessions.services.SessionUtilitysService;
import com.motomutterers.boardgames.skullking.dto.SkullKingStateResponse;
import com.motomutterers.boardgames.skullking.events.SkullKingStateChangedEvent;
import com.motomutterers.boardgames.skullking.services.SkullKingStateBuilder;

@Component
public class SkullKingListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomsUtilityService roomsUtilityService;
    private final SessionUtilitysService sessionUtilitysService;
    private final SkullKingStateBuilder stateBuilder;

    public SkullKingListener(
        SimpMessagingTemplate messagingTemplate,
        RoomsUtilityService roomsUtilityService,
        SessionUtilitysService sessionUtilitysService,
        SkullKingStateBuilder stateBuilder
    ){
        this.messagingTemplate = messagingTemplate;
        this.roomsUtilityService = roomsUtilityService;
        this.sessionUtilitysService = sessionUtilitysService;
        this.stateBuilder = stateBuilder;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStateChanged(SkullKingStateChangedEvent event){
        Room room = roomsUtilityService.getRoomByName(event.getRoomName());
        Session session = sessionUtilitysService.getOrThrowSessionByRoom(room);
        List<RoomUser> players = room.getPlayers();

        for(RoomUser player : players){
            boolean isAdmin = player.getRole().equals(RoomUserRoles.ADMIN);
            SkullKingStateResponse state = stateBuilder.buildState(session, player, isAdmin);

            if(isAdmin){
                messagingTemplate.convertAndSend(
                    "/topic/sessions/" + event.getRoomName() + "/admin",
                    state);
            } else if(player.getTeam() != null){
                messagingTemplate.convertAndSend(
                    "/topic/sessions/" + event.getRoomName() + "/teams/" + player.getTeam().getId(),
                    state);
            }
        }
    }
}
