package com.motomutterers.boardgames.auth.exceptions;

// The account exists but was created with the other sign-in method — a password
// account reached for via Google, or a Google account reached for via password.
// Accounts are never linked automatically, so the fix is always "use the other
// button".
public class AuthMethodMismatchException extends RuntimeException {
    public AuthMethodMismatchException(String message){
        super(message);
    }
}
