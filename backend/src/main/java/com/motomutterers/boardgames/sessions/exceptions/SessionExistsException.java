package com.motomutterers.boardgames.sessions.exceptions;

public class SessionExistsException extends RuntimeException {
    public SessionExistsException(String message){
        super(message);
    }
}
