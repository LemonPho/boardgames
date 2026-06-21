package com.motomutterers.boardgames.auth.exceptions;

public class RefreshTokenNotFoundException extends RuntimeException {
    public RefreshTokenNotFoundException(String message){
        super(message);
    }
}