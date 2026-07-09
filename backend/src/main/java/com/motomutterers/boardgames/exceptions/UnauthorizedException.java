package com.motomutterers.boardgames.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message){
        super(message);
    }
}
