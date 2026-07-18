package com.motomutterers.boardgames.auth.models;

import java.time.LocalDateTime;
import java.util.UUID;

import com.motomutterers.boardgames.user.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    string type          -- ACCOUNT_VERIFICATION | EMAIL_CHANGE
    string pending_email  -- target address for EMAIL_CHANGE, else null
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

    @Enumerated(EnumType.STRING)
    private VerificationTokenType type;

    private String pendingEmail;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public VerificationToken(){}

    // Account-verification token (registration).
    public VerificationToken(
        User user,
        String token,
        LocalDateTime expiresAt
    ) {
        this(user, token, VerificationTokenType.ACCOUNT_VERIFICATION, null, expiresAt);
    }

    // General constructor; use for EMAIL_CHANGE with the target address.
    public VerificationToken(
        User user,
        String token,
        VerificationTokenType type,
        String pendingEmail,
        LocalDateTime expiresAt
    ) {
        this.user = user;
        this.token = token;
        this.type = type;
        this.pendingEmail = pendingEmail;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId(){return this.id;}
    public User getUser(){return this.user;}
    public String getToken(){return this.token;}
    public VerificationTokenType getType(){return this.type;}
    public String getPendingEmail(){return this.pendingEmail;}
    public LocalDateTime getExpiresAt(){return this.expiresAt;}
    public LocalDateTime getCreatedAt(){return this.createdAt;}
}
