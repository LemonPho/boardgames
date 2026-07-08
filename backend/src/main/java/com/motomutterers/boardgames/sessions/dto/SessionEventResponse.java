package com.motomutterers.boardgames.sessions.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEventType;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.motomutterers.boardgames.sessions.models.sessionevent.SessionEvent;


public class SessionEventResponse {
    private SessionEventType type;
    private int sequence;

    @JsonRawValue
    private String payload;
    private LocalDateTime createdAt;

    public SessionEventResponse(
        SessionEvent sessionEvent
    ) {
        this.type = sessionEvent.getType();
        this.sequence = sessionEvent.getSequence();
        this.payload = sessionEvent.getPayload();
        this.createdAt = sessionEvent.getCreatedAt();
    }

    public SessionEventType getType(){return this.type;}
    public int getSequence(){return this.sequence;}
    public String getPayload(){return this.payload;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

}
