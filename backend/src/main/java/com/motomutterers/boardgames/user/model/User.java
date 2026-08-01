package com.motomutterers.boardgames.user.model;

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

    // Null for accounts created through Google — they authenticate with the
    // provider, never a local password. Guard with hasPassword().
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    // Google's immutable subject id. The identity key for Google accounts —
    // emails can be reassigned, this can't.
    @Column(unique = true)
    private String googleSub;

    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "user_status")
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "user_role")
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

    // Google-authenticated account: no password, and already active/verified —
    // Google has proven the address, so there's nothing left to email-confirm.
    public static User fromGoogle(String email, String username, String googleSub){
        User user = new User();
        user.email = email;
        user.username = username;
        user.passwordHash = null;
        user.authProvider = AuthProvider.GOOGLE;
        user.googleSub = googleSub;
        user.verified = true;
        user.status = UserStatus.ACTIVE;
        return user;
    }

    @PrePersist
    protected void onCreate() {
       this.createdAt = LocalDateTime.now();
       this.usernameLastEdited = null;
       // status is not reset here — the field default already covers local
       // registration, and Google accounts are created ACTIVE.
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

    public AuthProvider getAuthProvider(){
        return authProvider;
    }

    public String getGoogleSub(){
        return googleSub;
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

    public void setGoogleSub(String googleSub){
        this.googleSub = googleSub;
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

    // Whether local-password flows (login, change password, change email,
    // password reset) apply to this account. False for Google-only accounts,
    // which have no password to verify against.
    public boolean hasPassword(){
        return passwordHash != null;
    }

    public boolean isGoogleLinked(){
        return googleSub != null;
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
