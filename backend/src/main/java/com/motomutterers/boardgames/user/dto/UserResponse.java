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
}
