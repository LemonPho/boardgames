package com.motomutterers.boardgames.sessions.models.sessionevent;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.motomutterers.boardgames.sessions.models.session.Session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_events")
public class SessionEvent {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    private SessionEventType type;

    private int sequence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private LocalDateTime createdAt;

    public SessionEvent(){}
    public SessionEvent(
        Session session,
        SessionEventType type,
        int sequence,
        String payload
    ){
        this.session = session;
        this.type = type;
        this.sequence = sequence;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Session getSession(){return this.session;}
    public SessionEventType getType(){return this.type;}
    public int getSequence(){return this.sequence;}
    public String getPayload(){return this.payload;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setSession(Session session){this.session = session;}
    public void setType(SessionEventType type){this.type = type;}
    public void setSequence(int sequence){this.sequence = sequence;}
    public void setPayload(String payload){this.payload = payload;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
}
