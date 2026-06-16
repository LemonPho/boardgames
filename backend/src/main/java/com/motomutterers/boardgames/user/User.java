package com.motomutterers.boardgames.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private boolean verified = false;

    private LocalDateTime createdAt;

    private LocalDateTime usernameLastEdited;

    public User(
        String email,
        String username,
        String passwordHash
    ) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    protected void onCreate() {
       this.createdAt = LocalDateTime.now();
       this.usernameLastEdited = LocalDateTime.MIN;
    }

    public UUID getId(){
        return id;
    }

    public String getEmail(){
        return email;
    }

    public String getUsername(){
        return username;
    }

    public String getPasswordHash(){
        return passwordHash;
    }

    public boolean getVerified(){
        return verified;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public LocalDateTime getUsernameLastEdited(){
        return usernameLastEdited;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public boolean setUsername(String username){
        long daysSinceLastChange = ChronoUnit.DAYS.between(this.usernameLastEdited, LocalDateTime.now());
        if(daysSinceLastChange < 30){
            return false;
        }

        this.username = username;
        this.usernameLastEdited = LocalDateTime.now();

        return true;
    }
    
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public void setVerified(boolean verified){
        this.verified = verified;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUsernameLastEdited(LocalDateTime usernameLastEdited){
        this.usernameLastEdited = usernameLastEdited;
    }
}
