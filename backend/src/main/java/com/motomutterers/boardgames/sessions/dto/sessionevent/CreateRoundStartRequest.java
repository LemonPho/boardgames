package com.motomutterers.boardgames.sessions.dto.sessionevent;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;

public class CreateRoundStartRequest extends CreateSessionEventRequest {
    private int round;
    private int cardCount;

    public CreateRoundStartRequest(
        int sequence,
        String roomName,
        SessionEventType type,
        int round,
        int cardCount
    ) {
        this.setRoomName(roomName);
        this.setSequence(sequence);
        this.setType(type);

        this.round = round;
        this.cardCount = cardCount;
    }

    public int getRound(){return this.round;}
    public int getCardCount(){return this.cardCount;}
}
