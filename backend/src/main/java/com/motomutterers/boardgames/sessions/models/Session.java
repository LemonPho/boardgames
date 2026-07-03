package com.motomutterers.boardgames.sessions.models;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.rooms.model.Room.Room;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session {
    @GeneratedValue
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime endedAt;

    public Session(){}

    public Session(Room room){
        this.room = room;
        this.status = SessionStatus.IN_PROGRESS;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public Room getRoom(){return this.room;}
    public SessionStatus getStatus(){return this.status;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
    public LocalDateTime getEndedAt(){return this.endedAt;}

    public void setRoom(Room room){this.room = room;}
    public void setStatus(SessionStatus status){this.status = status;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public void setEndedAt(LocalDateTime endedAt){this.endedAt = endedAt;}
}
