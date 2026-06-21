package com.motomutterers.boardgames.auth.exceptions;

public class PasswordIncorrectException extends RuntimeException {
    public PasswordIncorrectException(String message){
        super(message);
    }
}
