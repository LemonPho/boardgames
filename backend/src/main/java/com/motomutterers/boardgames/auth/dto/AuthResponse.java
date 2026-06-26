package com.motomutterers.boardgames.auth.dto;

public class AuthResponse {
    private String accessToken;

    public AuthResponse(
        String accessToken
    ) {
        this.accessToken = accessToken;
    }

    public String getAccessToken(){
        return accessToken;
    }
}
