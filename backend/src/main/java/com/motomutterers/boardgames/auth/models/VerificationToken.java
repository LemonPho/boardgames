package com.motomutterers.boardgames.auth.models;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/*
VerificationTokens {
    uuid id
    uuid user_id FK
    string token
    timestamp expires_at
    timestamp created_at
  }
*/

@Entity
@Table(name = "verification_tokens")
public class VerificationToken {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public VerificationToken(){}
    public VerificationToken(
        User user,
        String token,
        LocalDateTime expiresAt
    ) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){
        return id;
    }

    public User getUser(){
        return user;
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
