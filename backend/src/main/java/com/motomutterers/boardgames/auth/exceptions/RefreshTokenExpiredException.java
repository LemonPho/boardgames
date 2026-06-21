package com.motomutterers.boardgames.auth.exceptions;

public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message){
        super(message);
    }
}
