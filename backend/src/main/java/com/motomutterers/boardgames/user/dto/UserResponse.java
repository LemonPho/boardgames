package com.motomutterers.boardgames.user.dto;

import java.time.LocalDateTime;

import com.motomutterers.boardgames.user.model.User;

public class UserResponse {
    private String username;
    private String email;
    private LocalDateTime createdAt;

    public UserResponse(){}

    public UserResponse(User user){
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
    }

    public void setUsername(String username) {this.username = username;}
    public void setEmail(String email) {this.email = email;}
    public void setCreatedAt(LocalDateTime createdAt) {this.createdAt = createdAt;}

    public String getUsername() {return this.username;}
    public String getEmail() {return this.email;}
    public LocalDateTime getCreatedAt() {return this.createdAt;}
}
