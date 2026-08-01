package com.motomutterers.boardgames.auth.exceptions;

// The Google ID token couldn't be trusted: bad signature, wrong audience or
// issuer, expired, or missing required claims.
public class GoogleAuthException extends RuntimeException {
    public GoogleAuthException(String message){
        super(message);
    }
}
