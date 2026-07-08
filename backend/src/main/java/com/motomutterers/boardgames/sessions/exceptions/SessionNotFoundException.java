package com.motomutterers.boardgames.sessions.exceptions;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(String message){
        super(message);
    }
}
