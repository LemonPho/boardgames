package com.motomutterers.boardgames.auth.exceptions;

public class VerificationTokenExpiredException extends RuntimeException {
    public VerificationTokenExpiredException(String message){
        super(message);
    }
}