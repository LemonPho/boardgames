package com.motomutterers.boardgames.sessions.listeners;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.rooms.listeners.RoomMessage;
import com.motomutterers.boardgames.sessions.dto.SessionEventResponse;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.events.SessionEventUpdatedEvent;
import com.motomutterers.boardgames.sessions.events.SessionUpdatedEvent;
import com.motomutterers.boardgames.sessions.events.TeamSessionEventUpdatedEvent;

@Component
public class SessionListener {
    private final String SESSION_UPDATED = "SESSION_UPDATED";
    private final String SESSION_EVENT = "SESSION_EVENT";
    private final SimpMessagingTemplate messagingTemplate;

    public SessionListener(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSessionUpdated(SessionUpdatedEvent sessionUpdatedEvent){
        SessionResponse response = new SessionResponse(sessionUpdatedEvent.getSession());
        messagingTemplate.convertAndSend(   "/topic/sessions/" + sessionUpdatedEvent.getRoomName(),
                                            new RoomMessage<>(SESSION_UPDATED, response));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSessionEventUpdated(SessionEventUpdatedEvent sessionEventUpdatedEvent){
        SessionEventResponse response = new SessionEventResponse(sessionEventUpdatedEvent.getSessionEvent());
        messagingTemplate.convertAndSend(   "/topic/sessions/" + sessionEventUpdatedEvent.getRoomName(),
                                            new RoomMessage<>(SESSION_EVENT, response));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTeamSessionEventUpdated(TeamSessionEventUpdatedEvent event){
        String roomName = event.getRoomName();
        String teamId = event.getTeamId().toString();

        // Send to the specific team's topic
        messagingTemplate.convertAndSend("/topic/sessions/" + roomName + "/teams/" + teamId, event.getTeamSessionEvent());

        // Also send to the admin topic so the admin sees all team updates
        messagingTemplate.convertAndSend("/topic/sessions/" + roomName + "/admin", event.getTeamSessionEvent());
    }
}
