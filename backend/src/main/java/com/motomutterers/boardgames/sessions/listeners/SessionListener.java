package com.motomutterers.boardgames.sessions.listeners;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.motomutterers.boardgames.rooms.listeners.RoomMessage;
import com.motomutterers.boardgames.sessions.dto.SessionResponse;
import com.motomutterers.boardgames.sessions.events.SessionUpdatedEvent;

@Component
public class SessionListener {
    private final String SESSION_UPDATED = "SESSION_UPDATED";
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
}
