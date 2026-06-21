package com.motomutterers.boardgames.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.motomutterers.boardgames.auth.dto.RegisterRequest;

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

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    private LocalDateTime createdAt;

    private LocalDateTime usernameLastEdited;

    public User(){}

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
       this.usernameLastEdited = null;
       this.status = UserStatus.PENDING_VERIFICATION;
    }

    public boolean canChangeUsername(){
        if(usernameLastEdited == null) return true;
        long daysSinceLastChange = ChronoUnit.DAYS.between(this.usernameLastEdited, LocalDateTime.now());
        return daysSinceLastChange >= 30;
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

    public UserStatus getStatus(){
        return status;
    }

    public UserRole getRole(){
        return role;
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

    public void setUsername(String username){
        this.username = username;
    }
    
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }

    public void setVerified(boolean verified){
        this.verified = verified;
    }
    
    private void setStatus(UserStatus status){
        this.status = status;
    }

    private void setRole(UserRole role){
        this.role = role;
    }

    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUsernameLastEdited(LocalDateTime usernameLastEdited){
        this.usernameLastEdited = usernameLastEdited;
    }

    //HELPERS
    public boolean isBanned(){
        return status.equals(UserStatus.BANNED);
    }

    public boolean isTemporarilyBanned(){
        return status.equals(UserStatus.TEMPORARILY_BANNED);
    }

    public boolean isDeleted(){
        return status.equals(UserStatus.DELETED);
    }

    public boolean isActive(){
        return status.equals(UserStatus.ACTIVE);
    }

    public boolean isPendingVerification(){
        return status.equals(UserStatus.PENDING_VERIFICATION);
    }

    public boolean isAdmin(){
        return role.equals(UserRole.ADMIN);
    }

    public void setIsPendingVerification(){
        setStatus(UserStatus.PENDING_VERIFICATION);
    }

    public void setIsActive(){
        setStatus(UserStatus.ACTIVE);
    }

    public void setIsTemporarilyBanned(){
        setStatus(UserStatus.TEMPORARILY_BANNED);
    }

    public void setIsBanned(){
        setStatus(UserStatus.BANNED);
    }

    public void setIsDeleted(){
        setStatus(UserStatus.DELETED);
    }

    public void setIsAdmin(){
        setRole(UserRole.ADMIN);
    }

    public void setIsUser(){
        setRole(UserRole.USER);
    }
}
