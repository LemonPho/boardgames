package com.motomutterers.boardgames.exceptions;

public class BadActionException extends RuntimeException {
    public BadActionException(String message){
        super(message);
    }
}
