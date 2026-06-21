package com.motomutterers.boardgames.auth.dto;

public class RefreshRequest {
    private String refreshToken;

    public RefreshRequest(){}
    public RefreshRequest(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken(){
        return refreshToken;
    }
}
