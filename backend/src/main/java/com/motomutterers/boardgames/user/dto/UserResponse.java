package com.motomutterers.boardgames.user.dto;

import com.motomutterers.boardgames.user.model.User;

public class UserResponse {
    private String username;
    private String email;

    public UserResponse(){}

    public UserResponse(User user){
        this.username = user.getUsername();
        this.email = user.getEmail();
    }

    public void setUsername(String username) {this.username = username;}
    public void setEmail(String email) {this.email = email;}

    public String getUsername() {return this.username;}
    public String getEmail() {return this.email;}
}
