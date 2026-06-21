package com.motomutterers.boardgames.auth.exceptions;

public class VerificationTokenNotFoundException extends RuntimeException {
    public VerificationTokenNotFoundException(String message){
        super(message);
    }
}