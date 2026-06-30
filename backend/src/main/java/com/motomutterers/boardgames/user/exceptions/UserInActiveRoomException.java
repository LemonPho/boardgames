package com.motomutterers.boardgames.user.exceptions;

public class UserInActiveRoomException extends RuntimeException{
    public UserInActiveRoomException(String message) {
        super(message);
    }
}