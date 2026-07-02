package com.motomutterers.boardgames.notifications.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/*
  Notifications {
    uuid id PK
    uuid user_id FK
    string type
    jsonb payload
    bool read
    timestamp created_at
  }
*/

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    private boolean read;

    private LocalDateTime createdAt;

    public Notification(){}

    public Notification(
        User user,
        NotificationType type,
        String payload
    ) {
        this.user = user;
        this.type = type;
        this.payload = payload;
        this.read = false;

        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public User getUser(){return this.user;}
    public NotificationType getType(){return this.type;}
    public String getPayload(){return this.payload;}
    public boolean getRead(){return this.read;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}

    public void setUser(User user){this.user = user;}
    public void setNotificationType(NotificationType type){this.type = type;}
    public void setPayload(String payload){this.payload = payload;}
    public void setRead(boolean read){this.read = read;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}

}
