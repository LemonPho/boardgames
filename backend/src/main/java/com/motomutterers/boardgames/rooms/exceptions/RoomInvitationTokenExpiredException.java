package com.motomutterers.boardgames.rooms.exceptions;

public class RoomInvitationTokenExpiredException extends RuntimeException {
    public RoomInvitationTokenExpiredException(String message){
        super(message);
    }
}
