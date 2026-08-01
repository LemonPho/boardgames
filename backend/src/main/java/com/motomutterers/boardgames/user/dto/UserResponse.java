package com.motomutterers.boardgames.user.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.user.model.AuthProvider;
import com.motomutterers.boardgames.user.model.User;

public class UserResponse {
    private String username;
    private String email;
    private LocalDateTime createdAt;
    // Lets the settings page hide the password/email sections for Google
    // accounts, which can only change their username.
    private AuthProvider authProvider;

    public UserResponse(){}

    public UserResponse(User user){
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
        this.authProvider = user.getAuthProvider();
    }

    public void setUsername(String username) {this.username = username;}
    public void setEmail(String email) {this.email = email;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}
    public void setAuthProvider(AuthProvider authProvider) {this.authProvider = authProvider;}

    public String getUsername() {return this.username;}
    public String getEmail() {return this.email;}
    public LocalDateTime getCreatedAt() {return this.createdAt;}
    public AuthProvider getAuthProvider() {return this.authProvider;}
}
