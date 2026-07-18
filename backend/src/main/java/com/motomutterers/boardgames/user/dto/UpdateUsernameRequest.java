package com.motomutterers.boardgames.user.dto;

import jakarta.validation.constraints.Pattern;

public class UpdateUsernameRequest {
    @Pattern(
        regexp = "^[a-zA-Z0-9 ._-]{3,18}$",
        message = "Username must be 3-18 characters and can only contain letters, numbers, spaces, periods, underscores and hyphens"
    )
    private String username;

    public UpdateUsernameRequest(){}

    public UpdateUsernameRequest(String username){
        this.username = username;
    }

    public String getUsername(){return this.username;}
    public void setUsername(String username){this.username = username;}
}
