package com.motomutterers.boardgames.rooms.model.Invitation;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.rooms.model.Room.Room;
import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_invitations_tokens")
public class RoomInvitationToken {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public RoomInvitationToken(){}
    public RoomInvitationToken(
        User user,
        Room room,
        String token,
        LocalDateTime expiresAt
    ) {
        this.user = user;
        this.room = room;
        this.status = InvitationStatus.PENDING;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public void setUser(User user){
        this.user = user;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setStatus(InvitationStatus status){
        this.status = status;
    }

    public void setToken(String token){
        this.token = token;
    }

    public void setExpiresAt(LocalDateTime expiresAt){
        this.expiresAt = expiresAt;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public UUID getId(){
        return id;
    }

    public User getUser(){
        return user;
    }

    public InvitationStatus getStatus(){
        return status;
    }

    public Room getRoom(){
        return room;
    }

    public String getToken(){
        return token;
    }

    public LocalDateTime getExpiresAt(){
        return expiresAt;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }
}
