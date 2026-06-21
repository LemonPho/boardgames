package com.motomutterers.boardgames.auth.dto;

public class LoginRequest {
    private boolean isUsername;
    private String primaryKey;
    private String password;

    public LoginRequest(
        boolean isUsername,
        String primaryKey,
        String password
    ) {
        this.isUsername = isUsername;
        this.primaryKey = primaryKey;
        this.password = password;
    }

    public boolean getIsUsername(){
        return isUsername;
    }

    public String getPrimaryKey(){
        return primaryKey;
    }

    public String getPassword(){
        return password;
    }
}
