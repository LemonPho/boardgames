package com.motomutterers.boardgames.auth.exceptions;

public class UserUnauthorizedException extends RuntimeException {
    public UserUnauthorizedException(String message){
        super(message);
    }
}
